package org.olgakhamzina.scientificlibrarythesis.server

import org.olgakhamzina.scientificlibrarythesis.shared.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.shared.WeightParams
import org.springframework.web.bind.annotation.*

/**
 * ParamController – контроллер для получения и обновления параметров расчёта рейтинга.
 * **Внимание:** В реальном приложении обновление параметров должно быть защищено авторизацией!
 */
@RestController
@RequestMapping("/params")
class ParamController(private val paramService: ParamService) {

    @GetMapping
    fun getParams(): ScoringParams {
        return paramService.getCurrentParams()
    }

    @PostMapping("/update")
    fun updateParams(@RequestBody newParams: WeightParams) {
        paramService.updateParams(newParams)
        // Возвращаем 200 OK при успехе. (Тело ответа не требуется.)
    }
}