package io.student.rangiffler.data;

import java.util.UUID;

/**
 * A user created in the auth database for one test. The password is kept in clear text so the test can sign in.
 */
public record TestUser(UUID id, String username, String password) {
}
