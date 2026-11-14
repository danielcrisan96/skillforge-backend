package ro.skillforge.api.conversation;

// Prețul modelului la momentul răspunsului, per milion de tokeni. Stă pe mesaj, nu se recitește dintr-un registru:
// o schimbare de preț nu trebuie să rescrie costul conversațiilor vechi.
public record Pricing(Double inputPerMillionUsd,
                      Double outputPerMillionUsd,
                      String verifiedAt) {
}
