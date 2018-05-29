package com.overtureone.entity;

public class Product {

    String number;
    String description;

    public Product(String number, String description) {
        this.number = number;
        this.description = description;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Product{" +
                "number='" + number + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
