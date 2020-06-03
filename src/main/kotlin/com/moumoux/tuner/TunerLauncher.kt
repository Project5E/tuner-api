package com.moumoux.tuner

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.impl.launcher.VertxCommandLauncher
import io.vertx.core.impl.launcher.VertxLifecycleHooks
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.runBlocking
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.io.FileInputStream
import java.util.*

class TunerLauncher: VertxCommandLauncher(), VertxLifecycleHooks {
    private val logger: Log = LogFactory.getLog(TunerLauncher::class.java)

    private val prop: Properties = Properties(System.getProperties()).also { prop ->
        FileInputStream("tuner.properties").use {
            prop.load(it)
        }
    }
    private val tunerConfig: JsonObject = runBlocking { getConfig() }

    override fun afterConfigParsed(config: JsonObject) {
        logger.info("afterConfigParsed called")
        config.mergeIn(tunerConfig)
    }

    private suspend fun getConfig(): JsonObject {
        val vertx = Vertx.vertx()
        val url = String.format(prop.getProperty("tuner.server"),
            prop.getProperty("tuner.namespace"),
            prop.getProperty("tuner.config"))
        val token = prop.getProperty("tuner.token")
        logger.debug(url)
        logger.debug("X-TUNER—TOKEN: $token")
        val webClient = WebClient.create(vertx)
        return try {
            awaitResult<HttpResponse<JsonObject>> { webClient
                .getAbs(url)
                .putHeader("X-TUNER-TOKEN", token)
                .`as`(BodyCodec.jsonObject())
                .send(it)
            }.body()
        } catch (e: Throwable) {
            logger.warn("Cannot retrieve config from Tuner")
            logger.warn(e)
            e.printStackTrace()
            JsonObject()
        } finally {
            webClient.close()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TunerLauncher().dispatch(args)
        }
    }

    override fun beforeStartingVertx(options: VertxOptions?) = Unit
    override fun afterStoppingVertx() = Unit
    override fun afterStartingVertx(vertx: Vertx?) = Unit
    override fun beforeStoppingVertx(vertx: Vertx?) = Unit
    override fun beforeDeployingVerticle(deploymentOptions: DeploymentOptions?) = Unit
    override fun handleDeployFailed(vertx: Vertx?, mainVerticle: String?, deploymentOptions: DeploymentOptions?, cause: Throwable?) = Unit
}
