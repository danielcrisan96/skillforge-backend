// Fiecare serviciu are propriul fișier de build; rădăcina doar rezolvă o singură dată versiunile pluginurilor.
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}
