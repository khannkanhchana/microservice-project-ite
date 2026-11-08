package kh.edu.istad.iam.application.port.out;

/** Provides the authenticated principal subject used for audit attribution. */
public interface CurrentActorProvider {
    String currentActor();
}
