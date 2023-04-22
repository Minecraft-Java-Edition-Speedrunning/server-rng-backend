package com.mcspeedrun.rng.repository

import com.mcspeedrun.rng.model.AuthenticationMethod
import com.mcspeedrun.rng.model.InstanceRegistration
import com.mcspeedrun.rng.model.Role
import com.mcspeedrun.rng.model.http.http500
import database.generated.server_rng.Tables.USERS
import database.generated.server_rng.Tables.REGISTERED_INSTANCES
import database.generated.server_rng.Tables.REGISTERED_ROLES
import jakarta.inject.Singleton
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.time.ZoneOffset

val INSTANCE_USERS = USERS
    .join(REGISTERED_INSTANCES)
    .on(
        USERS.ID.eq(REGISTERED_INSTANCES.USER_ID)
    )
@Singleton
class RegisteredInstancesRepository(
    private val jooq: DSLContext,
) {
    fun getUserIdentifier(authentication: AuthenticationMethod, userId: String): Long? {
        return jooq
            .select(USERS.ID)
            .from(USERS)
            .where(
                USERS.AUTHENTICATION.eq(authentication.id),
                USERS.USER_ID.eq(userId),
            )
            .fetchOne()
            ?.component1()
    }

    fun createUserIdentifier(authentication: AuthenticationMethod, userId: String) : Long {
        return jooq
            .insertInto(USERS, USERS.AUTHENTICATION, USERS.USER_ID)
            .values(authentication.id, userId)
            .returning()
            .fetchOne()
            ?.id ?: throw http500("unable to create user identifier")
    }

    fun getRegisteredInstances(identifier: Long): List<InstanceRegistration> {
        return jooq
            .selectFrom(INSTANCE_USERS)
            .where(
                USERS.ID.eq(identifier),
                REGISTERED_INSTANCES.INVALIDATED.eq(0),
            )
            .fetchInto(InstanceRegistration::class.java)
    }

    fun trimRegisteredInstances(identifier: Long) {
        jooq
            .update(REGISTERED_INSTANCES)
            .set(REGISTERED_INSTANCES.INVALIDATED, 1)
            .where(REGISTERED_INSTANCES.USER_ID.eq(identifier))
            .orderBy(REGISTERED_INSTANCES.REFRESHED_AT.asc())
            .limit(1)
            .execute()
    }

    fun refreshInstance(instanceId: String) {
        jooq
            .update(REGISTERED_INSTANCES)
            .set(REGISTERED_INSTANCES.REFRESHED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(REGISTERED_INSTANCES.ID.eq(instanceId))
            .execute()
    }

    fun getRoles(instanceId: String): List<Role> {
        return jooq
            .select(REGISTERED_ROLES.ROLE)
            .from(REGISTERED_ROLES)
            .where(REGISTERED_ROLES.INSTANCE_ID.eq(instanceId))
            .fetch()
            .map { Role.values().find { role: Role -> role.id == it.component1() } }
    }

    fun saveRegisteredInstance(uuid: String, identifier: Long, refreshToken: String, roles: List<Role> = listOf()) {
        jooq
            .insertInto(
                REGISTERED_INSTANCES,
                REGISTERED_INSTANCES.ID,
                REGISTERED_INSTANCES.USER_ID,
                REGISTERED_INSTANCES.REFRESH_TOKEN,
            )
            .values(uuid, identifier, refreshToken)
            .execute()
        val roleInsert = jooq.insertInto(REGISTERED_ROLES, REGISTERED_ROLES.INSTANCE_ID, REGISTERED_ROLES.ROLE)
        roles.fold(roleInsert) { insertStep, role -> insertStep.values(uuid, role.id) }.execute()
    }

    fun startRun(instanceId: String) {
        // TODO("make sure last run is at least 2 seconds ago and update flag if it is")
    }
}
