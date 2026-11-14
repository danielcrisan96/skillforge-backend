package ro.skillforge.api.user;

// Un fapt anunțat în interiorul aplicației când un utilizator își șterge contul. Pachetul conversation îl ascultă și
// își șterge datele; așa, user nu trebuie să știe ce alte pachete țin date despre un utilizator.
public record UserDeleted(String userId) {
}
