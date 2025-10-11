package de.healthai.eatelligent.data

import android.content.Context
import de.healthai.eatelligent.Gender
import de.healthai.eatelligent.UserConfiguration
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.json.JSONObject

class UserConfigurationStorage(context: Context) {
    private val file = File(context.filesDir, "user_configuration.json")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun read(): UserConfiguration? {
        if (!file.exists()) return null
        val content = file.readText().takeIf { it.isNotBlank() } ?: return null
        val json = JSONObject(content)
        val name = json.optString("name", "")
        val diagnosis = json.optString("diagnosis", "")
        val genderValue = json.optString("gender", Gender.Girl.name)
        val gender = runCatching { Gender.valueOf(genderValue) }.getOrDefault(Gender.Girl)
        val birthday = if (json.isNull("birthday")) {
            null
        } else {
            json.optString("birthday")
                .takeIf { it.isNotBlank() }
                ?.let { LocalDate.parse(it, dateFormatter) }
        }
        return UserConfiguration(
            name = name,
            birthday = birthday,
            gender = gender,
            diagnosis = diagnosis
        )
    }

    @Throws(IOException::class)
    fun write(configuration: UserConfiguration) {
        val json = JSONObject().apply {
            put("name", configuration.name)
            if (configuration.birthday != null) {
                put("birthday", configuration.birthday.format(dateFormatter))
            } else {
                put("birthday", JSONObject.NULL)
            }
            put("gender", configuration.gender.name)
            put("diagnosis", configuration.diagnosis)
        }
        file.writeText(json.toString())
    }
}
