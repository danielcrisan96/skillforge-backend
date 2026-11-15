# Google Cloud Run

**Link către dashboard:** https://console.cloud.google.com/run

## Ce face

Rulează imaginea Docker a backend-ului de persistență și îi dă o adresă https, pe care o apelează serverul Next de pe Vercel. Scalează la zero când nu e folosit.

## Unde rulează acum

| Ce               | Valoare                                                         |
| ---------------- | --------------------------------------------------------------- |
| Proiect          | `skillforge-daniel-2026`                                        |
| Regiune          | `europe-west3` (Frankfurt), aproape de clusterul Atlas din Frankfurt |
| Serviciu         | `skillforge-api`, rulează ca `skillforge-run`                   |
| Depozit imagini  | `europe-west3-docker.pkg.dev/skillforge-daniel-2026/skillforge` |
| Secrete          | `mongodb-password`, `skillforge-api-key`                        |
| Adresa serviciului | `gcloud run services describe skillforge-api --region europe-west3 --format='value(status.url)'` |

## Cont & chei

- Unde se creează contul: https://console.cloud.google.com
- Ce plan e necesar: un cont Google cu billing activat (perioada de probă e suficientă). Necesită card.
- Unelte pe laptop: `gcloud`, Docker Desktop și, pentru pipeline, opțional `gh`.
- Instalarea `gcloud` pe macOS: `curl https://sdk.cloud.google.com | bash`, apoi `exec -l $SHELL` și `gcloud auth login`.
- Nu se generează nicio cheie de service account. Pipeline-ul se autentifică prin Workload Identity Federation: GitHub semnează un token la fiecare rulare, iar Google îl schimbă pe o oră de drepturi.

## Variabile de mediu

Variabilele repository-ului de GitHub, citite de `.github/workflows/build.yml`. Sunt **variabile, nu secrete**: numesc lucruri, nu deschid nimic.

| Variabilă          | Ce conține                                   | Obligatorie   |
| ------------------ | -------------------------------------------- | ------------- |
| `GCP_PROJECT_ID`   | Id-ul proiectului Google Cloud               | pentru deploy |
| `GCP_REGION`       | Regiunea: `europe-west3`                     | pentru deploy |
| `GCP_WIF_PROVIDER` | Numele complet al providerului de identitate | pentru deploy |
| `GCP_DEPLOYER`     | Emailul contului de serviciu care deployează | pentru deploy |

Fără ele, jobul `deploy` e sărit și rularea rămâne verde.

## Pași manuali

Comenzile sunt pentru terminalul de macOS (zsh), rulate din rădăcina acestui repo, câte una pe rând. Valorile dintre `<...>` le înlocuiești. Pe Windows, în Git Bash, vezi nota de la pasul 4.

> **Toată secvența se face în aceeași fereastră de terminal.** Variabilele create cu `export` există doar în fereastra în care au fost create. Într-o fereastră nouă sunt goale, iar comenzile pică cu erori ca `invalid tag "/skillforge-api:first"`.

**0. Proiectul și facturarea** (o singură dată):

```bash
gcloud projects create skillforge-daniel-2026
gcloud billing accounts list
gcloud billing projects link skillforge-daniel-2026 --billing-account=<ACCOUNT_ID>
gcloud billing projects describe skillforge-daniel-2026 --format="value(billingEnabled)"
```

Ultima comandă trebuie să afișeze `True`. Un proiect creat din terminal nu e legat automat de facturare. Pune și o alertă de buget: Console → Billing → **Budgets & alerts**.

**1. Variabilele terminalului.** `REPO` se creează după `PROJECT_ID` și `REGION`, fiindcă le folosește.

```bash
export PROJECT_ID=skillforge-daniel-2026
export REGION=europe-west3
export REPO=$REGION-docker.pkg.dev/$PROJECT_ID/skillforge
export MONGODB_HOST=<cluster>.mongodb.net
export MONGODB_USERNAME=skillforge
```

Parola MongoDB se citește fără să apară pe ecran (sintaxa e cea din zsh; în bash: `read -rsp "Parola MongoDB: " MONGODB_PASSWORD`):

```bash
read -rs "MONGODB_PASSWORD?Parola MongoDB: "; echo; export MONGODB_PASSWORD
export SKILLFORGE_API_KEY=$(openssl rand -hex 32)
echo "Proiect: $PROJECT_ID | Regiune: $REGION | Repo: $REPO | Parola: ${#MONGODB_PASSWORD} caractere | Cheie: ${#SKILLFORGE_API_KEY} caractere"
```

Parola și cheia sunt necesare doar la pasul 3, când se salvează în Secret Manager. **Nu genera o cheie nouă într-o fereastră nouă**: ar fi diferită de cea salvată. Pentru pașii 4–6 ajung variabilele fără parolă și cheie.

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
gcloud secrets list
```

`gcloud secrets list` afișează doar numele, niciodată valorile.

**4. Prima imagine și primul deploy, de mână.** Pipeline-ul schimbă doar imaginea unui serviciu care există deja.

`--platform linux/amd64` e obligatoriu pe un Mac cu procesor Apple: Cloud Run rulează pe amd64. Build-ul rulează emulat și poate dura 5–15 minute.

```bash
docker build --platform linux/amd64 --build-arg SERVICE=skillforge-api --build-arg PORT=8090 -t $REPO/skillforge-api:first .
docker push $REPO/skillforge-api:first
```

```bash
gcloud run deploy skillforge-api \
  --image $REPO/skillforge-api:first \
  --region $REGION \
  --service-account skillforge-run@$PROJECT_ID.iam.gserviceaccount.com \
  --allow-unauthenticated \
  --port 8090 --cpu 1 --memory 512Mi \
  --min-instances 0 --max-instances 2 \
  --set-env-vars SPRING_PROFILES_ACTIVE=cloud,MONGODB_HOST=$MONGODB_HOST,MONGODB_USERNAME=$MONGODB_USERNAME \
  --set-secrets MONGODB_PASSWORD=mongodb-password:latest,SKILLFORGE_API_KEY=skillforge-api-key:latest \
  --startup-probe httpGet.path=/actuator/health/readiness,httpGet.port=8090,periodSeconds=5,timeoutSeconds=3,failureThreshold=24
```

La final apare `Service URL: https://skillforge-api-….run.app`. Pe Windows, în Git Bash, pune `MSYS2_ARG_CONV_EXCL="httpGet.path="` în fața lui `gcloud run deploy`: altfel Git Bash transformă `/actuator/...` într-o cale de Windows.

De ce `--allow-unauthenticated`: apelantul e serverul Next de pe Vercel, care e în afara Google Cloud și nu are un token Google. Serviciul e deci public la nivel de rețea și închis prin cheia dintre servicii, verificată în `ApiKeyFilter`.

**5. Leagă aplicația web.** În Vercel → Settings → Environment Variables, pentru **Production**:

- `SKILLFORGE_API_URL` = adresa afișată de `gcloud run services describe skillforge-api --region $REGION --format='value(status.url)'`, fără „/” la final;
- `SKILLFORGE_API_KEY` = cheia. Se copiază direct din Secret Manager în clipboard, fără să apară pe ecran, și se lipește în Vercel:

```bash
gcloud secrets versions access latest --secret=skillforge-api-key | pbcopy
```

Apoi un deploy nou în Vercel (Redeploy, sau un push pe `main` în `skillforge-demo`). Variabilele noi nu ajung într-un deploy deja făcut.

**6. Pipeline-ul, fără nicio cheie** (o singură dată pe proiect, opțional):

```bash
export GITHUB_REPO=danielcrisan96/skillforge-backend
export PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
export DEPLOYER=github-deployer@$PROJECT_ID.iam.gserviceaccount.com
gcloud iam workload-identity-pools create github --location=global --display-name="GitHub Actions"
gcloud iam workload-identity-pools providers create-oidc github-actions --location=global --workload-identity-pool=github --issuer-uri=https://token.actions.githubusercontent.com --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" --attribute-condition="assertion.repository=='$GITHUB_REPO' && assertion.ref=='refs/heads/main'"
gcloud iam service-accounts create github-deployer --display-name="GitHub Actions deployer"
gcloud artifacts repositories add-iam-policy-binding skillforge --location=$REGION --member="serviceAccount:$DEPLOYER" --role=roles/artifactregistry.writer
gcloud projects add-iam-policy-binding $PROJECT_ID --member="serviceAccount:$DEPLOYER" --role=roles/run.developer --condition=None
gcloud iam service-accounts add-iam-policy-binding skillforge-run@$PROJECT_ID.iam.gserviceaccount.com --member="serviceAccount:$DEPLOYER" --role=roles/iam.serviceAccountUser
gcloud iam service-accounts add-iam-policy-binding $DEPLOYER --role=roles/iam.workloadIdentityUser --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/$GITHUB_REPO"
echo "GCP_WIF_PROVIDER = projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/providers/github-actions"
echo "GCP_DEPLOYER     = $DEPLOYER"
```

`GITHUB_REPO` trebuie scris exact ca pe GitHub: providerul îl compară literă cu literă. Condiția de pe provider (`assertion.repository` și `assertion.ref`) este lacătul: fără ea, orice repository de pe GitHub ar putea prezenta un token valid.

Cele patru variabile ale repository-ului se pun în GitHub → repo → **Settings → Secrets and variables → Actions → Variables → New repository variable**: `GCP_PROJECT_ID` = `skillforge-daniel-2026`, `GCP_REGION` = `europe-west3`, plus cele două valori afișate de ultimele `echo`. Cu `gh` instalat, se poate și din terminal: `gh variable set GCP_PROJECT_ID --body "$PROJECT_ID"` și la fel pentru celelalte trei.

**Rollback:** fiecare deploy creează o revizie nouă, iar o revizie nu se schimbă. Întoarcerea e o mutare de trafic, nu un rebuild:

```bash
gcloud run revisions list --service skillforge-api --region $REGION --format="table(metadata.name,metadata.labels.commit,metadata.creationTimestamp)"
gcloud run services update-traffic skillforge-api --region $REGION --to-revisions <revizia>=100
```

**Rotația secretelor:** `printf '%s' "<valoarea nouă>" | gcloud secrets versions add <secret> --data-file=-`, apoi un redeploy, ca serviciul să citească versiunea nouă. La rotația cheii dintre servicii, valoarea se schimbă și în Vercel.

## Cost & limite

- Model de tarifare: CPU și memorie, cât timp se servește o cerere; cu `--min-instances 0`, serviciul nu costă cât timp nu e folosit.
- Consecința scalării la zero: prima cerere după o pauză așteaptă pornirea JVM-ului, câteva secunde (cold start). Înainte de o demonstrație, fă o cerere de încălzire: `curl -s <adresa serviciului>/actuator/health`.
- Pagina oficială de prețuri: https://cloud.google.com/run/pricing
- **Verificat la data:** neverificat la scrierea acestui document. Citește pragurile gratuite din pagina de mai sus înainte să te bazezi pe ele.
- Unde se vede consumul curent: Console → Billing → Reports, filtrat pe proiect.

## Verificare

1. `curl -s <adresa serviciului>/actuator/health` afișează `{"status":"UP",...}`.
2. `curl -s -o /dev/null -w "%{http_code}\n" <adresa serviciului>/conversations` afișează `401`: fără cheie nu se servește nimic.
3. `curl -s <adresa serviciului>/conversations -H "X-Api-Key: $(gcloud secrets versions access latest --secret=skillforge-api-key)" -H "X-User-Id: github:test"` afișează `[]`.
4. După un push pe `main`, cu pipeline-ul configurat, toate joburile din Actions sunt verzi, ultimul fiind `deploy`, iar eticheta `commit` a reviziei noi este SHA-ul commit-ului.

## Când ceva nu merge

| Ce vezi | De ce | Ce faci |
| --- | --- | --- |
| `invalid tag "/skillforge-api:first"` sau `-docker.pkg.dev//skillforge` | Fereastră nouă de terminal: variabilele sunt goale | Rulează din nou pasul 1, fără cheie nouă |
| Deploy-ul pică la startup probe | Backend-ul nu ajunge la MongoDB: parola greșită sau `0.0.0.0/0` lipsă din Atlas → Network Access | `gcloud run services logs read skillforge-api --region $REGION --limit 50` |
| Proiectul nu apare în consolă | Consola arată alt proiect selectat | Click pe numele proiectului din bara de sus → tabul **All** |
| `gcloud services enable` pică cu o eroare de billing | Proiectul nu e legat de facturare | Pasul 0 |
