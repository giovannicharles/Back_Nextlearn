package com.nextcommunity.nextlearn.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String nom;
    private String prenom;
    private String classe;
    private String email;
    private String userrole;

    public UserDto(Long id, String nom, String prenom, String classe,String email, String userrole) {
        this.id = id;
        this.nom = nom;
        this.prenom=prenom;
        this.classe = classe;
        this.email = email;
        this.userrole = userrole;
    }

<<<<<<< HEAD
=======
    public UserDto() {

    }

>>>>>>> 465e66e (update backend NextLearn)
    public String getUserrole() {
        return userrole != null ? userrole.trim() : null;
    }


}
