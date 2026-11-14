# MongoDB Atlas

**Link către dashboard:** https://cloud.mongodb.com

## Ce face

E baza de date a backend-ului de persistență în cloud: ține colecțiile `users` și `conversations`. Local, același rol îl are MongoDB din `compose.yml`, fără niciun cont.

## Cont & chei

- Unde se creează contul: https://www.mongodb.com/cloud/atlas/register
- Ce plan e necesar: clusterul gratuit e suficient pentru proiect.
- Pași care nu sunt evidenți: clusterul se creează într-un **Project**; utilizatorul bazei de date (**Database Access**) e altceva decât contul tău de Atlas.
- De unde se generează credențialele: Security → **Database Access** → Add New Database User → Password. Rol: citire și scriere pe orice bază de date.
- **Se afișează o singură dată?** Da, parola generată se vede doar la creare. Copiaz-o atunci.

## Variabile de mediu

Sunt ale backend-ului (profilul `cloud`), nu ale aplicației web:

| Variabilă de mediu | Ce conține                                                   | Unde                            | Obligatorie |
| ------------------ | ------------------------------------------------------------ | ------------------------------- | ----------- |
| `MONGODB_HOST`     | Gazda clusterului, de forma `<cluster>.mongodb.net` (config) | variabilă de mediu pe Cloud Run | da          |
| `MONGODB_USERNAME` | Utilizatorul bazei de date (config)                          | variabilă de mediu pe Cloud Run | da          |
| `MONGODB_PASSWORD` | Parola lui (**secret**)                                      | Secret Manager                  | da          |
| `MONGODB_DATABASE` | Numele bazei de date; implicit `skillforge`                  | variabilă de mediu              | nu          |

Parola nu se scrie niciodată într-un fișier și nici pe linia de comandă.

## Pași manuali

- Security → **Network Access** → Add IP Address → `0.0.0.0/0`. Cloud Run nu are o adresă fixă, deci baza de date trebuie să accepte conexiuni de oriunde; ce o apără e parola.
- Gazda se citește din Database → **Connect** → Drivers: e partea dintre `@` și `/` din connection string.
- Rotația parolei: Database Access → Edit → Edit Password, apoi o versiune nouă a secretului `mongodb-password` și un redeploy al backend-ului.

## Cost & limite

- Model de tarifare: gratuit pe clusterul de intrare, cu spațiu de stocare limitat; plătit pe cele dedicate.
- Pagina oficială de prețuri: https://www.mongodb.com/pricing
- **Verificat la data:** neverificat la scrierea acestui document. Citește limitele planului gratuit din pagina de mai sus înainte să te bazezi pe ele.
- Unde se vede consumul curent: Atlas → clusterul tău → Metrics; dimensiunea datelor apare în Collections.

## Verificare

1. Cu backend-ul pornit pe profilul `cloud`, salvează o conversație din aplicație.
2. În Atlas → Browse Collections, baza `skillforge` are colecțiile `users` și `conversations`, iar conversația apare cu mesajele încorporate și cu câmpul `version`.
3. Cu parola greșită, backend-ul nu trece de pornire: probe-ul de readiness eșuează, iar în log apare o eroare de autentificare MongoDB. Cu Network Access neconfigurat, pornește, dar orice cerere expiră.
