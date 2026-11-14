# Google Cloud Run

**Link către dashboard:** https://console.cloud.google.com/run

## Ce face

Rulează imaginea Docker a backend-ului de persistență și îi dă o adresă https, pe care o apelează serverul Next de pe Vercel. Scalează la zero când nu e folosit.

## Cont & chei

- Unde se creează contul: https://console.cloud.google.com
- Ce plan e necesar: un cont Google cu billing activat (perioada de probă e suficientă). Necesită card.
- Unelte pe laptop: `gcloud`, Docker Desktop și, pentru pipeline, `gh`.
- Nu se generează nicio cheie de service account. Pipeline-ul se autentifică prin Workload Identity Federation: GitHub semnează un token la fiecare rulare, iar Google îl schimbă pe o oră de drepturi.

## Variabile de mediu

Variabilele repository-ului de GitHub, citite de `.github/workflows/build.yml`. Sunt **variabile, nu secrete**: numesc lucruri, nu deschid nimic.

| Variabilă          | Ce conține                                   | Obligatorie   |
| ------------------ | -------------------------------------------- | ------------- |
| `GCP_PROJECT_ID`   | Id-ul proiectului Google Cloud               | pentru deploy |
| `GCP_REGION`       | Regiunea, de exemplu `europe-west1`          | pentru deploy |
| `GCP_WIF_PROVIDER` | Numele complet al providerului de identitate | pentru deploy |
| `GCP_DEPLOYER`     | Emailul contului de serviciu care deployează | pentru deploy |

Fără ele, jobul `deploy` e sărit și rularea rămâne verde.

## Pași manuali

Rulează în Git Bash, din rădăcina acestui repo, câte o comandă pe rând. Valorile dintre `<...>` le înlocuiești. Parolele se citesc fără să fie afișate.

**1. Variabilele terminalului** (din nou în fiecare terminal nou):

```bash
export PROJECT_ID=<id-ul-proiectului>
export REGION=europe-west1
export REPO=$REGION-docker.pkg.dev/$PROJECT_ID/skillforge
export MONGODB_HOST=<cluster>.mongodb.net
export MONGODB_USERNAME=<utilizatorul bazei de date>
read -rsp "Parola MongoDB: " MONGODB_PASSWORD; echo; export MONGODB_PASSWORD
export SKILLFORGE_API_KEY=$(openssl rand -base64 32)
```

**2. Proiectul, API-urile, depozitul de imagini:**

```bash
gcloud config set project $PROJECT_ID
gcloud config set run/region $REGION
gcloud services enable run.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com iam.googleapis.com logging.googleapis.com iamcredentials.googleapis.com sts.googleapis.com
gcloud artifacts repositories create skillforge --repository-format=docker --location=$REGION
gcloud auth configure-docker $REGION-docker.pkg.dev --quiet
```

**3. Cele două secrete și contul sub care rulează serviciul:**

```bash
printf '%s' "$MONGODB_PASSWORD" | gcloud secrets create mongodb-password --data-file=- --replication-policy=automatic
printf '%s' "$SKILLFORGE_API_KEY" | gcloud secrets create skillforge-api-key --data-file=- --replication-policy=automatic
gcloud iam service-accounts create skillforge-run --display-name="skillforge-api on Cloud Run"
gcloud secrets add-iam-policy-binding mongodb-password --member="serviceAccount:skillforge-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/secretmanager.secretAccessor
gcloud secrets add-iam-policy-binding skillforge-api-key --member="serviceAccount:skillforge-run@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/secretmanager.secretAccessor
```

**4. Prima imagine și primul deploy, de mână.** Pipeline-ul schimbă doar imaginea unui serviciu care există deja. `MSYS2_ARG_CONV_EXCL` oprește Git Bash din a transforma `/actuator/...` într-o cale de Windows.

```bash
docker build --platform linux/amd64 --build-arg SERVICE=skillforge-api --build-arg PORT=8090 -t $REPO/skillforge-api:first .
docker push $REPO/skillforge-api:first
MSYS2_ARG_CONV_EXCL="httpGet.path=" gcloud run deploy skillforge-api --image $REPO/skillforge-api:first --service-account skillforge-run@$PROJECT_ID.iam.gserviceaccount.com --allow-unauthenticated --port 8090 --cpu 1 --memory 512Mi --min-instances 0 --max-instances 2 --set-env-vars SPRING_PROFILES_ACTIVE=cloud,MONGODB_HOST=$MONGODB_HOST,MONGODB_USERNAME=$MONGODB_USERNAME --set-secrets MONGODB_PASSWORD=mongodb-password:latest,SKILLFORGE_API_KEY=skillforge-api-key:latest --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8090,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

De ce `--allow-unauthenticated`: apelantul e serverul Next de pe Vercel, care e în afara Google Cloud și nu are un token Google. Serviciul e deci public la nivel de rețea și închis prin cheia dintre servicii, verificată în `ApiKeyFilter`.

**5. Leagă aplicația web.** Pune în Vercel adresa tipărită de comanda de mai jos (`SKILLFORGE_API_URL`) și cheia (`SKILLFORGE_API_KEY`), apoi Redeploy. Cheia se copiază din terminal direct în Vercel:

```bash
gcloud run services describe skillforge-api --format='value(status.url)'
echo "$SKILLFORGE_API_KEY"
```

**6. Pipeline-ul, fără nicio cheie** (o singură dată pe proiect):

```bash
export GITHUB_REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
export PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
export DEPLOYER=github-deployer@$PROJECT_ID.iam.gserviceaccount.com
gcloud iam workload-identity-pools create github --location=global --display-name="GitHub Actions"
gcloud iam workload-identity-pools providers create-oidc github-actions --location=global --workload-identity-pool=github --issuer-uri=https://token.actions.githubusercontent.com --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" --attribute-condition="assertion.repository=='$GITHUB_REPO' && assertion.ref=='refs/heads/main'"
gcloud iam service-accounts create github-deployer --display-name="GitHub Actions deployer"
gcloud artifacts repositories add-iam-policy-binding skillforge --location=$REGION --member="serviceAccount:$DEPLOYER" --role=roles/artifactregistry.writer
gcloud projects add-iam-policy-binding $PROJECT_ID --member="serviceAccount:$DEPLOYER" --role=roles/run.developer --condition=None
gcloud iam service-accounts add-iam-policy-binding skillforge-run@$PROJECT_ID.iam.gserviceaccount.com --member="serviceAccount:$DEPLOYER" --role=roles/iam.serviceAccountUser
gcloud iam service-accounts add-iam-policy-binding $DEPLOYER --role=roles/iam.workloadIdentityUser --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/$GITHUB_REPO"
gh variable set GCP_PROJECT_ID --body "$PROJECT_ID"
gh variable set GCP_REGION --body "$REGION"
gh variable set GCP_WIF_PROVIDER --body "projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/providers/github-actions"
gh variable set GCP_DEPLOYER --body "$DEPLOYER"
```

Condiția de pe provider (`assertion.repository` și `assertion.ref`) este lacătul: fără ea, orice repository de pe GitHub ar putea prezenta un token valid.

**Rollback:** fiecare deploy creează o revizie nouă, iar o revizie nu se schimbă. Întoarcerea e o mutare de trafic, nu un rebuild:

```bash
gcloud run revisions list --service skillforge-api --format="table(metadata.name,metadata.labels.commit,metadata.creationTimestamp)"
gcloud run services update-traffic skillforge-api --to-revisions <revizia>=100
```

**Rotația secretelor:** `gcloud secrets versions add <secret> --data-file=-`, apoi un redeploy, ca serviciul să citească versiunea nouă.

## Cost & limite

- Model de tarifare: CPU și memorie, cât timp se servește o cerere; cu `--min-instances 0`, serviciul nu costă cât timp nu e folosit.
- Consecința scalării la zero: prima cerere după o pauză așteaptă pornirea JVM-ului, câteva secunde (cold start). Înainte de o demonstrație, fă o cerere de încălzire.
- Pagina oficială de prețuri: https://cloud.google.com/run/pricing
- **Verificat la data:** neverificat la scrierea acestui document. Citește pragurile gratuite din pagina de mai sus înainte să te bazezi pe ele.
- Unde se vede consumul curent: Console → Billing → Reports, filtrat pe proiect. Pune o alertă de buget (Billing → Budgets & alerts) înainte de primul deploy.

## Verificare

1. `curl -s -o /dev/null -w "%{http_code}\n" <adresa serviciului>/actuator/health` tipărește `200`.
2. `curl -s -o /dev/null -w "%{http_code}\n" <adresa serviciului>/conversations` tipărește `401`: fără cheie nu se servește nimic.
3. După un push pe `main`, toate joburile din Actions sunt verzi, ultimul fiind `deploy`, iar eticheta `commit` a reviziei noi este SHA-ul commit-ului.
4. Dacă probe-ul de pornire eșuează și în log apare `C:/Program Files/Git/actuator/...`, comanda de deploy a fost rulată fără `MSYS2_ARG_CONV_EXCL`.
