package models

import java.time.LocalDateTime

/**
 * Represents the many-to-many relationship between a User and a Role.
 * This model maps directly to the `user_roles` join table.
 *
 * @param userId The ID of the user.
 * @param roleId The ID of the role assigned to the user.
 * @param createdAt The timestamp when this role was assigned.
 */
case class UserRoles(userId: Long, roleId: Long, createdAt: LocalDateTime)
