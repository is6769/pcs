package ru.mirea.cloud.model;

public final class Client {
    private final long id;
    private final String name;
    private final String email;
    private final String company;

    public Client(long id, String name, String email, String company) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.company = company;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCompany() { return company; }

    @Override public String toString() {
        return "%d | %s | %s | %s".formatted(id, name, email, company);
    }
}
