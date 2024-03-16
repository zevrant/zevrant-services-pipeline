import com.zevrant.services.pojo.NotificationChannel
import com.zevrant.services.services.NotificationService

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

ZonedDateTime estTime
ZonedDateTime arizonaTime


pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
    }

    stages {
        stage("Determine Next Session") {
            steps {
                script {
                    estTime = ZonedDateTime.now(ZoneId.of(ZoneOffset.SHORT_IDS.get('EST')))
                    estTime.get
                    arizonaTime = now.withZoneSameInstant(ZoneId.of(ZoneId.SHORT_IDS.get('MST')))
                }
            }
        }

        stage ('Send Message to Discord') {
            steps {
                script {
                    new NotificationService(this).sendDiscordNotification("""@everyone 
The next session is scheduled for:
    * ${estTime.format(DateTimeFormatter.RFC_1123_DATE_TIME)}
    * ${arizonaTime.format(DateTimeFormatter.RFC_1123_DATE_TIME)} 
""", "Session Reminder", NotificationChannel.DISCORD_CICD)
                }
            }
        }
    }
}