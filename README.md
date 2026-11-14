# SkillForge API

Backend-ul de persistență al SkillForge: un serviciu Spring Boot care păstrează în MongoDB profilul și conversațiile utilizatorilor conectați. Aplicația web ([skillforge-demo](https://github.com/danielcrisan96/skillforge-demo), Next.js) îl apelează de pe server; browserul nu ajunge niciodată direct la el.

```
browser → Next.js pe Vercel (verifică sesiunea) → skillforge-api (8090) → MongoDB
```

Stack: Java 25, Spring Boot 4, Spring Data MongoDB, springdoc (OpenAPI), Testcontainers, Docker.

## Pornire locală

Ai nevoie de JDK 25 și de Docker.

```
docker compose up -d mongodb
./gradlew test
./gradlew :skillforge-api:bootRun --args='--spring.profiles.active=local'
```

Serviciul ascultă pe `http://localhost:8090`. Profilul `local` folosește cheia de dezvoltare `local-dev-key`.

- Cereri de probă: `http/skillforge.http` (IntelliJ IDEA Ultimate, sau VS Code cu extensia REST Client).
- Documentația API-ului: `http://localhost:8090/swagger-ui.html`. Apasă **Authorize** și pune cheia; fiecare cerere cere și headerul `X-User-Id`.
- Tot backend-ul în containere, cu o singură comandă: `docker compose up --build`.

Ca aplicația web să-l folosească local, pune în `.env.local` din proiectul `skillforge-demo` `SKILLFORGE_API_URL=http://localhost:8090` și `SKILLFORGE_API_KEY=local-dev-key`. Pașii compleți sunt în `docs/backend/README.md` din acel proiect.

## API

Toate cererile cer `X-Api-Key` (cheia dintre servicii) și `X-User-Id` (utilizatorul, pus de serverul Next după ce a verificat sesiunea).

| Metodă | Cale                  | Ce face                                                     | Răspunsuri         |
| ------ | --------------------- | ----------------------------------------------------------- | ------------------ |
| GET    | `/users/me/profile`   | Profilul salvat                                             | 200, 404           |
| PUT    | `/users/me/profile`   | Salvează profilul; prima salvare creează utilizatorul       | 200, 400           |
| DELETE | `/users/me`           | Șterge profilul și toate conversațiile                      | 204                |
| GET    | `/conversations`      | Conversațiile utilizatorului, cea mai recent salvată prima  | 200                |
| GET    | `/conversations/{id}` | O conversație, cu mesajele ei                               | 200, 404           |
| PUT    | `/conversations/{id}` | Salvează instantaneul unei conversații                      | 200, 400, 404, 409 |
| DELETE | `/conversations/{id}` | Șterge o conversație                                        | 204, 404           |
| GET    | `/usage`              | Tokenii consumați, pe model și pe preț                      | 200                |

Fără cheie: 401. Fără cheie configurată pe server: 503. Erorile au forma `ProblemDetail` (`application/problem+json`).

## Structura codului

```
ro.skillforge.api
├── user          User, Profile, Skill, UserController, UserService, UserRepository, UserMapper,
│                 UserDeleted, UserNotFoundException, dto/ ProfileRequest, ProfileResponse
├── conversation  Conversation, Message, MessagePart, MessageMetadata, Pricing, TokenUsage,
│                 ConversationController, UsageController, ConversationService, ConversationRepository,
│                 UsageReportRepository(+Impl), ConversationMapper, ConversationNotFoundException,
│                 dto/ SaveConversationRequest, ConversationResponse, ModelUsageResponse
├── shared        error/ NotFoundException, GlobalExceptionHandler; web/ ApiKeyFilter
└── config        MongoConfig, OpenApiConfig
```

Pe fiecare funcționalitate: controller → service → repository, cu DTO-uri la margine. Entitățile nu ies din service.

## Deciziile, pe scurt

**Mesajele sunt încorporate în conversație; conversația referă utilizatorul.** Mesajele se citesc mereu împreună cu conversația, iar aplicația web le salvează ca instantaneu, o singură dată, la finalul răspunsului: o conversație este o singură scriere, atomică. Conversațiile stau în colecția lor, cu `userId` indexat, pentru că un utilizator poate avea oricâte.

**Profilul este încorporat în utilizator.** E mic, are o limită și nu se citește niciodată fără el.

**Id-urile vin din afară.** Utilizatorul are id-ul stabil construit de Auth.js (`github:<id numeric>`), iar conversația are id-ul generat de aplicația web. De aceea salvările sunt `PUT`: aceeași cerere trimisă de două ori lasă serverul în aceeași stare.

**Optimistic locking pe conversație.** Două ferestre deschise pe aceeași conversație ar putea salva una peste cealaltă. `Conversation` are `@Version`, iar entitatea pleacă spre baza de date cu versiunea trimisă de client: fereastra rămasă în urmă primește 409. Nu există `@Retryable`: o reîncercare ar citi versiunea nouă și ar scrie peste mesajele celeilalte ferestre. `ConcurrentSaveTest` dovedește comportamentul; scoate `@Version` și testul pică.

**Raportul de consum e o agregare.** `UsageReportRepositoryImpl` adună tokenii în MongoDB (`$match`, `$unwind`, `$match`, `$project`, `$group`). Prețul face parte din cheia grupării, iar costul nu se calculează aici: formula are un singur loc, în aplicația web (`src/lib/cost.ts`).

**Validare în două locuri.** La ușă, `@Valid` pe body răspunde cu 400. Înainte de fiecare salvare, `ValidatingEntityCallback` verifică entitatea, ca un bug al nostru să nu ajungă în baza de date.

**Cine are voie.** Sesiunea e verificată în Next.js. Backend-ul crede headerul `X-User-Id` doar pentru că cererea poartă și cheia dintre servicii (`ApiKeyFilter`, comparație în timp constant). Fără cheie configurată, serviciul rămâne închis (503). Fiecare citire caută după id și după utilizator: conversația altcuiva răspunde 404, nu 403.

**Date personale.** Se salvează id-ul contului și ce a scris omul: profilul și conversațiile. Emailul, numele și poza contului nu ajung aici. `DELETE /users/me` șterge tot; pachetul `conversation` află printr-un eveniment intern (`UserDeleted`).

**Ce nu are și de ce.** Un singur serviciu: nu există încă un al doilea care să justifice un broker de mesaje sau un gateway, iar limita de cereri e deja în ruta de chat din Next. Extinderea planificată este un serviciu de statistici care primește prin RabbitMQ câte un mesaj pentru fiecare conversație salvată.

## Teste

| Test                                              | Fel                                      | Ce dovedește                                                  |
| ------------------------------------------------- | ---------------------------------------- | ------------------------------------------------------------- |
| `ConversationServiceTest`, `UserServiceTest`      | Persistență, pe MongoDB real             | Salvare, versiuni, izolare între utilizatori, agregare        |
| `ConcurrentSaveTest`                              | Persistență                              | Din 10 salvări simultane cu aceeași versiune câștigă una      |
| `ConversationControllerTest`, `UserControllerTest`| `@WebMvcTest`, serviciul e un mock       | Coduri HTTP, validare, filtrul de cheie                       |
| `SkillforgeApiApplicationTest`                    | `@SpringBootTest`                        | Aplicația pornește întreagă                                   |

Testele de persistență își pornesc propriul MongoDB cu Testcontainers, deci Docker trebuie să ruleze.

## Deploy

Imaginea se construiește cu `Dockerfile` (două etape: JDK pentru build, JRE pentru rulare, utilizator fără drepturi, layere). Rularea în cloud e descrisă în [`deploy/cloud-run.md`](deploy/cloud-run.md), iar baza de date în [`deploy/mongodb-atlas.md`](deploy/mongodb-atlas.md). Workflow-ul `.github/workflows/build.yml` compilează, testează și construiește imaginea la fiecare push; jobul de deploy rulează doar pe `main` și doar după ce variabilele sunt configurate.
