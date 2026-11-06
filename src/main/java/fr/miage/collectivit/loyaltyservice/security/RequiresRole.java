package fr.miage.collectivit.loyaltyservice.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indiquant les rôles autorisés à exécuter un point d'entrée API. Lorsqu'elle est
 * présente sur une méthode ou une classe de contrôleur, l'intercepteur exige un jeton JWT valide et
 * vérifie que le rôle de l'utilisateur fait partie des rôles autorisés.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
  String[] value();
}
