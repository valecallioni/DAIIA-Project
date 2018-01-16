package Helpers;

import java.io.Serializable;

public class User implements Serializable {

    private enum Gender {
        Female,
        Male
    }

    private enum Occupation {
        Student,
        Professor,
        Employee,
        Employer,
        SelfEmployee
    }

    private int age;
    private Occupation occupation;
    private Gender gender;

    private int interestCentury;
    private Creator interestCreator;
    private Genre interestGenre;

    //Constructor
    public User() {
        age = (int) (Math.random() * 60 + 16);

        int occupationIndex = (int) (Math.random() * 4);
        occupation = Occupation.values()[occupationIndex];

        int genderIndex = (int) (Math.random() * 1);
        gender = Gender.values()[genderIndex];

        interestCentury = (int) (Math.random() * 500 + 1300);
        interestCentury = interestCentury - (interestCentury % 100);

        int creatorIndex = (int) (Math.random() * 8);
        interestCreator = Creator.values()[creatorIndex];

        int randomGenreIndex = (int) (Math.random() * 4);
        interestGenre = Genre.values()[randomGenreIndex];
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Occupation getOccupation() {
        return occupation;
    }

    public void setOccupation(Occupation occupation) {
        this.occupation = occupation;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getInterestCentury() {
        return interestCentury;
    }

    public void setInterestCentury(int interestCentury) {
        this.interestCentury = interestCentury;
    }

    public Creator getInterestCreator() {
        return interestCreator;
    }

    public void setInterestCreator(Creator interestCreator) {
        this.interestCreator = interestCreator;
    }

    public Genre getInterestGenre() {
        return interestGenre;
    }

    public void setInterestGenre(Genre interestGenre) {
        this.interestGenre = interestGenre;
    }

}
