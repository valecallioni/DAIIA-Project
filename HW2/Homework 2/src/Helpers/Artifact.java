package Helpers;

import java.io.Serializable;

public class Artifact implements Serializable {

    private long id;
    private String name;
    private int creationYear;
    private Creator creator;
    private Genre genre;
    private int price;

    // Constructor
    public Artifact(){

        // To be sure that we do not have duplicates we generate the ids
        // using a range between 0 and MAXVALUE
        id = (long) (Math.random() * Long.MAX_VALUE);
        name = "Artifact" + id;
        creationYear = (int) (Math.random()*500+1300);


        int creatorIndex = (int) (Math.random()*8);
        creator = Creator.values()[creatorIndex];

        int genreIndex = (int) (Math.random()*4);
        genre = Genre.values()[genreIndex];

        price = (int) (Math.random() * 10000 + 100);
        price = price - (price % 100);

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Creator getCreator() {
        return creator;
    }

    public void setCreator(Creator creator) {
        this.creator = creator;
    }

    public int getCreationYear() {
        return creationYear;
    }

    public void setCreationYear(int creationYear) {
        this.creationYear = creationYear;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }


    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

}
