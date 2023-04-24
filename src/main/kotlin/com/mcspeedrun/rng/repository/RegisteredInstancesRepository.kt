package com.mcspeedrun.rng.repository

import com.mcspeedrun.rng.model.AuthenticationMethod
import com.mcspeedrun.rng.model.InstanceRegistration
import com.mcspeedrun.rng.model.Access
import com.mcspeedrun.rng.model.http.http425
import com.mcspeedrun.rng.model.http.http500
import database.generated.server_rng.Tables.USERS
import database.generated.server_rng.Tables.REGISTERED_INSTANCES
import database.generated.server_rng.Tables.REGISTERED_ACCESS
import jakarta.inject.Singleton
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.time.ZoneOffset

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
            .select(
                REGISTERED_INSTANCES.ID,
                REGISTERED_INSTANCES.USER_ID,
                USERS.AUTHENTICATION,
                REGISTERED_INSTANCES.REFRESH_TOKEN_KEY,
                REGISTERED_INSTANCES.INVALIDATED,
                REGISTERED_INSTANCES.REFRESHED_AT,
                REGISTERED_INSTANCES.CREATED_AT,
                REGISTERED_INSTANCES.LAST_RUN_AT,
            )
            .from(USERS)
            .join(REGISTERED_INSTANCES)
            .on(
                USERS.ID.eq(REGISTERED_INSTANCES.USER_ID)
            )
            .where(
                USERS.ID.eq(identifier),
                REGISTERED_INSTANCES.INVALIDATED.eq(0),
            )
            .also { println(it.fetch()) }
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

    fun refreshInstance(instanceId: String, refreshTokenKey: String) {
        jooq
            .update(REGISTERED_INSTANCES)
            .set(REGISTERED_INSTANCES.REFRESHED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(REGISTERED_INSTANCES.REFRESH_TOKEN_KEY, refreshTokenKey)
            .where(REGISTERED_INSTANCES.ID.eq(instanceId))
            .execute()
    }

    fun getRefreshTokenInstance(refreshTokenKey: String): String? {
        return jooq
            .select(REGISTERED_INSTANCES.ID)
            .from(REGISTERED_INSTANCES)
            .where(REGISTERED_INSTANCES.REFRESH_TOKEN_KEY.eq(refreshTokenKey))
            .fetchOne()?.component1()
    }

    fun getAccess(instanceId: String): List<Access> {
        return jooq
            .select(REGISTERED_ACCESS.ACCESS)
            .from(REGISTERED_ACCESS)
            .where(REGISTERED_ACCESS.INSTANCE_ID.eq(instanceId))
            .fetch()
            .map { Access.values().find { access: Access -> access.id == it.component1() } }
    }

    fun saveRegisteredInstance(
        uuid: String,
        identifier: Long,
        refreshTokenKey: String,
        accesses: List<Access> = listOf()
    ) {
        jooq
            .insertInto(
                REGISTERED_INSTANCES,
                REGISTERED_INSTANCES.ID,
                REGISTERED_INSTANCES.USER_ID,
                REGISTERED_INSTANCES.REFRESH_TOKEN_KEY,
            )
            .values(uuid, identifier, refreshTokenKey)
            .execute()
        val roleInsert = jooq.insertInto(REGISTERED_ACCESS, REGISTERED_ACCESS.INSTANCE_ID, REGISTERED_ACCESS.ACCESS)
        accesses.fold(roleInsert) { insertStep, role -> insertStep.values(uuid, role.id) }.execute()
    }

    fun startRun(instanceId: String) {
        jooq.transactionResult { transaction ->
            val lastRunAt = transaction.dsl()
                .select(REGISTERED_INSTANCES.LAST_RUN_AT).from(REGISTERED_INSTANCES)
                .where(REGISTERED_ACCESS.INSTANCE_ID.eq(instanceId))
                .fetchOne()
                ?.component1()
            val now = LocalDateTime.now()
            // TODO("make the amount of time here configurable")
            if (lastRunAt?.isBefore(now.minusSeconds(2)) != false) {
                transaction.dsl()
                    .update(REGISTERED_INSTANCES)
                    .set(REGISTERED_INSTANCES.LAST_RUN_AT, now)
                    .where(REGISTERED_ACCESS.INSTANCE_ID.eq(instanceId))
                    .execute()
            } else {
                throw http425("to early to start another run")
            }
        }
    }
}
