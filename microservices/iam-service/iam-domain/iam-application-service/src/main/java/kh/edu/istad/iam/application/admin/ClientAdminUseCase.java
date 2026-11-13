package kh.edu.istad.iam.application.admin;

import kh.edu.istad.iam.application.admin.AdminModels.ClientCommand;
import kh.edu.istad.iam.application.admin.AdminModels.ClientDetails;
import kh.edu.istad.iam.application.admin.AdminModels.ClientWithSecret;

import java.util.List;

/** Registration and maintenance of OAuth2/OIDC clients. */
public interface ClientAdminUseCase {

    List<ClientDetails> list(String search);

    ClientDetails get(String id);

    /** Confidential clients get a generated secret, returned once. */
    ClientWithSecret create(ClientCommand command, String actor);

    /** The secret is non-null only when the update turned a public client into a confidential one. */
    ClientWithSecret update(String id, ClientCommand command, String actor);

    void delete(String id, String actor);

    ClientWithSecret rotateSecret(String id, String actor);
}
