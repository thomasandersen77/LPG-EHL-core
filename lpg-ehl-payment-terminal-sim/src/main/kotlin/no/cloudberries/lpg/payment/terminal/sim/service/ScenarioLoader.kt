package no.cloudberries.lpg.payment.terminal.sim.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.scenario.ScenarioDefinition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

@Service
class ScenarioLoader(
    private val config: SimulatorConfig
) {
    private val log = LoggerFactory.getLogger(ScenarioLoader::class.java)
    private val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory())
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun loadScenario(name: String): ScenarioDefinition? {
        if (!config.scenariosEnabled) {
            return null
        }

        val normalizedName = name.uppercase()
        val fileNames = listOf("$normalizedName.yml", "$normalizedName.yaml")

        loadFromExternalPath(fileNames)?.let { return it }
        return loadFromClasspath(fileNames)
    }

    private fun loadFromExternalPath(fileNames: List<String>): ScenarioDefinition? {
        val externalRoot = config.scenariosPath?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val rootPath = Path.of(externalRoot)
        if (!Files.exists(rootPath)) {
            log.debug("Scenario path not found: {}", rootPath)
            return null
        }

        for (fileName in fileNames) {
            val filePath = rootPath.resolve(fileName)
            if (Files.exists(filePath)) {
                return try {
                    yamlMapper.readValue(filePath.toFile(), ScenarioDefinition::class.java)
                } catch (ex: Exception) {
                    log.warn("Failed to read scenario YAML from {}: {}", filePath, ex.message)
                    null
                }
            }
        }

        return null
    }

    private fun loadFromClasspath(fileNames: List<String>): ScenarioDefinition? {
        val classLoader = javaClass.classLoader
        for (fileName in fileNames) {
            val resource = classLoader.getResource("scenarios/$fileName") ?: continue
            return try {
                yamlMapper.readValue(resource, ScenarioDefinition::class.java)
            } catch (ex: Exception) {
                log.warn("Failed to read scenario YAML from classpath {}: {}", resource, ex.message)
                null
            }
        }

        return null
    }
}