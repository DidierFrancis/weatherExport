package sun

import com.google.common.io.Files
import grails.gorm.transactions.Transactional
import grails.io.IOUtils
import grails.plugins.rest.client.RestBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import org.apache.commons.lang.time.DateUtils
import org.grails.web.json.JSONObject
import org.ini4j.Ini
import org.ini4j.IniPreferences
import sun.constante.Constantes
import sun.exception.SaqException
import sun.helpers.DateUtilExtensions
import sun.wheatherExport.Forecast
import sun.wheatherExport.Helper
import sun.wheatherExport.PathFile
import sun.wheatherExport.UserGateway

import java.util.prefs.Preferences


@Transactional
class GatewayService {

    def getDateRapport() {

        def FILE_DATE = PathFile.findByName("FILE_MATRIX_PATH")
        if (!FILE_DATE)
            throw new SaqException("4004", "FILE_MATRIX_PATH not found !")
        Ini ini = new Ini(new File(FILE_DATE.path + "/rapport.ini"));
        Preferences prefs = new IniPreferences(ini);

        def dateTrends = prefs.node("rapport")?.get("date", null)


        Ini ini1 = new Ini(new File("/MARINELEC/config.ini"));
        Preferences prefs1 = new IniPreferences(ini1);
        def apiKeys = prefs1.node("API_KEYS")?.get("GOOGLE_MAP", null)

        println(apiKeys: apiKeys)
        println(props: System.getProperty("rapport"))
        def strDate = System.getProperty("rapport") ?: dateTrends
        def date = DateUtils.parseDate(strDate, DateUtilExtensions.listPattern())
        Helper helper = new Helper()
        helper.dateRapport = date
        helper.googleApiKey = apiKeys
        helper.save()

    }

    def loginToDTNApi() {
        def authUrl = Constantes.DNT_AUTH_URL
        def username = Constantes.DNT_AUTH_USERNAME
        def password = Constantes.DNT_AUTH_PASSWORD
        def userGateway = UserGateway.findByCode(Constantes.DNT_CODE)
        if (userGateway && new Date().after(DateUtilExtensions.getDateAtNextHour(userGateway.tokenDate))) {
            userGateway.delete()
            //return userGateway.token
        }
        def resp = login(username, password, authUrl)
        userGateway = new UserGateway()
        userGateway.username = username
        userGateway.password = password.encodeAsSHA256()
        userGateway.urlConnexion = authUrl
        userGateway.token = resp.access_token
        userGateway.code = Constantes.DNT_CODE
        userGateway.save()
        return resp.access_token

    }

    @Transactional
    def getDTNForecast() {

        def FILE_DATE = PathFile.findByName("FILE_MATRIX_PATH")
        if (!FILE_DATE)
            throw new SaqException("4004", "FILE_MATRIX_PATH not found !")

        Ini ini = new Ini(new File(FILE_DATE.path + "/rapport.ini"));
        Preferences prefs = new IniPreferences(ini);

        def dateTrends = prefs.node("rapport")?.get("date", null)

        def reqStartDate = Helper.first()?.dateRapport ?: DateUtils.parseDate(dateTrends, DateUtilExtensions.listPattern())
        reqStartDate = DateUtilExtensions.minus(reqStartDate, 1)
        def reqEndDate = DateUtilExtensions.plus(reqStartDate, 3)
        def formatStartDate = DateUtilExtensions.format(reqStartDate, "yyyy-MM-dd")
        def formatEndDate = DateUtilExtensions.format(reqEndDate, "yyyy-MM-dd")

        def userGateway = UserGateway.findByCode(Constantes.DNT_CODE)
        if (!userGateway.token) {
            userGateway.token = loginToDTNApi()
        }
        // Generate file
        def urlKeys = Constantes.DNT_KEYS_PATH_URL.replaceAll("_STARTDATE_", formatStartDate).replaceAll("_ENDDATE_", formatEndDate)
        def keys = routerCall(userGateway.token, urlKeys, 2)
        if (keys) {
            keys.each {
                log.info(JsonOutput.toJson([methode: 'GetForecastkeys', val: it, formatStartDate: formatStartDate, formatEndDate: formatEndDate]))
            }
        }

        if (!keys.isEmpty()) {
            def urlForecaste = Constantes.DNT_BY_DATE_PATH_URL.replaceAll("_KEYS_", "${keys[0].key}")
            def result = routerCall(userGateway.token, urlForecaste, 0)
            if (result.Forecasts) {

                result.Forecasts.each {

                    def forecast = new Forecast()

                    forecast.latitude = it.Latitude
                    forecast.longitude = it.Longitude
                    forecast.dateTime = it.ForecastDateTime
                    forecast.windDirection10 = it.WindDirection10
                    forecast.windSpeed10 = it.WindSpeed10
                    forecast.windSpeedRisk10 = it.WindSpeedRisk10
                    forecast.windSpeedGusts10 = it.WindSpeedGusts10
                    forecast.windSpeed50 = it.WindSpeed50
                    forecast.windSpeedGusts50 = it.WindSpeedGusts50
                    forecast.windSpeed100 = it.WindSpeed100
                    forecast.windSpeedGusts100 = it.WindSpeedGusts100
                    forecast.windWaveHeight = it.WindWaveHeight
                    forecast.windWavePeakPeriod = it.WindWavePeakPeriod
                    forecast.swellHeight = it.SwellHeight
                    forecast.swellDirection = it.SwellDirection
                    forecast.swellPeakPeriod = it.SwellPeakPeriod
                    forecast.totalWaveMeanDirection = it.TotalWaveMeanDirection
                    forecast.totalWaveHeight = it.TotalWaveHeight
                    forecast.totalWaveRiskHeight = it.TotalWaveRiskHeight
                    forecast.totalWaveMaximumHeight = it.TotalWaveMaximumHeight
                    forecast.totalWavePeakPeriod = it.TotalWavePeakPeriod
                    forecast.surfaceCurrentSpeed = it.SurfaceCurrentSpeed
                    forecast.surfaceCurrentDirection = it.SurfaceCurrentDirection
                    forecast.weatherType = it.WeatherType
                    forecast.seaSurfaceTemperature = it.SeaSurfaceTemperature
                    forecast.airTemperature10 = it.AirTemperature10
                    forecast.visibility10 = it.Visibility10
                    forecast.probabilityOfPrecipitation = it.ProbabilityOfPrecipitation
                    forecast.meanSeaLevelPressure = it.MeanSeaLevelPressure
                    forecast.probabilityOfFog = it.ProbabilityOfFog
                    forecast.lightningProbability = it.LightningProbability
                    forecast.save(flush: true)
                }


            }
        }
    }


    def login(String username, String password, String url) {

        log.info("Login To The Gateway")
        String authUrl = url
        def rest = new RestBuilder()
        def resp = rest.post("${authUrl}") {
            accept("application/json")
            contentType("application/json")
            auth(username, password)
        }

        log.info("Authentification " + resp.body.toString())
        if (resp.status == 401) {
            throw new Exception("login ou mot de passe incorrect")
        }
        if (resp.status != 200 && resp?.json?.toString()?.equals("{}")) {
            throw new Exception("Une erreur est survenue côté serveur lors de l'authentification")
        }


        def json = resp.json.toString()
        def slurper = new JsonSlurper()
        def result = slurper.parseText(json ?: "{}")
        return result
    }

    def routerCall(String token, String url, def type) {
        def rest = new RestBuilder()

        log.info("url => $url")
        if (type == 0) {
            def resp = rest.get("${url}") {
                accept("application/json")
                contentType("application/json")
                auth("Bearer ${token}")
                accept("application/json")
            }

            if (resp.status == 401) {
                throw new Exception("Token invalide")
            }

            if (resp.status != 200) {
                throw new Exception("Une erreur est survenue côté serveur lors de la recuperation: status code: $resp.status")
            }


            def json = resp.body.toString().substring(1, resp.body.toString().length() - 1)
            def slurper = new JsonSlurper()
            def result = slurper.parseText(json ?: "{}")
            return result ?: []
        } else if (type == 1) {
            def client = HttpClient.create(Constantes.DNT_BASE_URL.toURL())
            HttpRequest request = HttpRequest.GET("${Constantes.DNT_PATH_URL}/type/${type}")
            request.headers('authorization': "Bearer ${token} ", 'contentType': "application/json")
            def result = client.toBlocking().retrieve(request, byte[].class)

            def FILE_DTN = PathFile.findByName("FILE_DTN")
            if (!FILE_DTN)
                throw new SaqException("4004", "FILE_DTN not found !")

            def fileName = FILE_DTN.path
            def file = new File(fileName)
            Files.write(result, file)
            return null

        } else if (type == 2) {

            def resp = rest.get("${url}") {
                accept("application/json")
                contentType("application/json")
                auth("Bearer ${token}")
                accept("application/json", "application/x-www-form-urlencoder")
            }

            if (resp.status == 401) {
                throw new Exception("Token invalide")
            }

            if (resp.status != 200 && resp.status != 201) {
                println(body: resp.body)
                println(token: token)
                return []
                //    throw new Exception("Une erreur est survenue côté serveur lors de la recuperation: status code: $resp.status")
            }

            def json = resp.json.toString()
            def slurper = new JsonSlurper()
            def result = slurper.parseText(json ?: "{}")
            return result
        }

    }

}
