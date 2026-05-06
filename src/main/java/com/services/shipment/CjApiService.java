package com.services.shipment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class CjApiService {

    // CJ API 기본 URL을 application.properties에서 가져온다.
    @Value("${cj.api.base-url}")
    private String baseUrl;

    // CJ 고객사 코드를 application.properties에서 가져온다.
    @Value("${cj.api.cust-id}")
    private String custId;

    // CJ에 등록된 사업자번호를 application.properties에서 가져온다.
    @Value("${cj.api.biz-reg-num}")
    private String bizRegNum;

    // 발급받은 토큰번호를 서버 메모리에 임시 저장한다.
    private String tokenNum;

    // 발급받은 토큰의 만료시간을 서버 메모리에 임시 저장한다.
    private String tokenExpireTime;

    /*
     * 유효한 토큰을 가져오는 메서드다.
     * 이미 토큰이 있으면 기존 토큰을 사용하고,
     * 토큰이 없으면 CJ에 새 토큰을 요청한다.
     */
    public Map<String, Object> getValidToken() {

        // tokenNum이 있으면 이미 발급받은 토큰이 있다는 뜻이다.
        if (tokenNum != null && !"".equals(tokenNum)) {
            Map<String, Object> result = new HashMap<>();

            result.put("success", true);
            result.put("message", "기존 토큰 사용");
            result.put("tokenNum", tokenNum);
            result.put("tokenExpireTime", tokenExpireTime);

            return result;
        }

        // 저장된 토큰이 없으면 새 토큰을 발급받는다.
        return requestOneDayToken();
    }

    /*
     * CJ 1Day 토큰 발급 API를 호출하는 메서드다.
     * CUST_ID와 BIZ_REG_NUM을 보내고 TOKEN_NUM을 응답으로 받는다.
     */
    public Map<String, Object> requestOneDayToken() {

        // 토큰 발급 API URL을 만든다.
        String apiUrl = baseUrl + "/ReqOneDayToken";

        // CJ 문서 기준 토큰 발급 요청 body다.
        // TOKEN_NUM은 요청값이 아니라 응답으로 받는 값이다.
        String requestBody = "{\n" +
                "  \"DATA\": {\n" +
                "    \"CUST_ID\": \"" + custId + "\",\n" +
                "    \"BIZ_REG_NUM\": \"" + bizRegNum + "\"\n" +
                "  }\n" +
                "}";

        // 실제 HTTP 호출은 공통 메서드에서 처리한다.
        // 토큰 발급 API는 CJ-Gateway-APIKey 없이 호출한다.
        Map<String, Object> result = callCjApi(apiUrl, requestBody, null);

        // CJ 응답 원문을 문자열로 꺼낸다.
        String responseBody = String.valueOf(result.get("responseBody"));

        // CJ 응답에서 결과코드와 결과메시지를 꺼낸다.
        String resultCd = extractValue(responseBody, "RESULT_CD");
        String resultDetail = extractValue(responseBody, "RESULT_DETAIL");

        // 화면이나 테스트 결과에서 보기 쉽게 결과값을 담는다.
        result.put("resultCd", resultCd);
        result.put("resultDetail", resultDetail);

        // CJ 기준 성공은 HTTP 200이 아니라 RESULT_CD가 S인 경우다.
        if ("S".equals(resultCd)) {

            // 토큰 발급 성공 시 응답에서 토큰번호와 만료시간을 꺼낸다.
            tokenNum = extractValue(responseBody, "TOKEN_NUM");
            tokenExpireTime = extractValue(responseBody, "TOKEN_EXPRTN_DTM");

            result.put("success", true);
            result.put("message", "토큰 발급 성공");
            result.put("tokenNum", tokenNum);
            result.put("tokenExpireTime", tokenExpireTime);

        } else {

            // RESULT_CD가 S가 아니면 업무적으로 실패 처리한다.
            result.put("success", false);
            result.put("message", resultDetail);
        }

        return result;
    }

    /*
     * CJ API를 실제로 호출하는 공통 메서드다.
     * 토큰 발급, 주소정제, 운송장 생성, 예약접수 등에서 재사용한다.
     */
    private Map<String, Object> callCjApi(String apiUrl, String requestBody, String apiKey) {

        Map<String, Object> result = new HashMap<>();
        HttpURLConnection conn = null;

        try {
            // 문자열 URL을 자바 URL 객체로 변환한다.
            URL url = new URL(apiUrl);

            // 해당 URL로 HTTP 연결을 준비한다.
            conn = (HttpURLConnection) url.openConnection();

            // CJ API는 POST 방식으로 호출한다.
            conn.setRequestMethod("POST");

            // 요청 body가 JSON 형식임을 알린다.
            conn.setRequestProperty("Content-Type", "application/json");

            // 응답도 JSON 형식으로 받겠다고 알린다.
            conn.setRequestProperty("Accept", "application/json");

            // 토큰 발급 이후 다른 API 호출 시에는 토큰을 헤더에 넣는다.
            if (apiKey != null && !"".equals(apiKey)) {
                conn.setRequestProperty("CJ-Gateway-APIKey", apiKey);
            }

            // requestBody를 전송할 수 있도록 설정한다.
            conn.setDoOutput(true);

            // requestBody 문자열을 UTF-8로 변환해서 CJ 서버로 전송한다.
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes("UTF-8"));
                os.flush();
            }

            // CJ 서버에서 내려준 HTTP 응답 코드를 받는다.
            int responseCode = conn.getResponseCode();

            // HTTP 코드가 400 이상이면 에러 스트림, 아니면 정상 스트림을 읽는다.
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            "UTF-8"
                    )
            );

            // CJ 응답 내용을 한 줄씩 읽어서 하나의 문자열로 합친다.
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            br.close();

            // HTTP 통신 성공 여부를 따로 담는다.
            // 실제 CJ 업무 성공 여부는 RESULT_CD로 다시 판단한다.
            result.put("httpSuccess", responseCode >= 200 && responseCode < 300);

            // HTTP 응답 코드를 담는다.
            result.put("responseCode", responseCode);

            // CJ 응답 원문을 담는다.
            result.put("responseBody", sb.toString());

        } catch (Exception e) {

            // 통신 중 예외가 발생하면 실패 정보와 에러 메시지를 담는다.
            result.put("success", false);
            result.put("httpSuccess", false);
            result.put("error", e.getMessage());

        } finally {

            // 연결 객체가 만들어졌다면 반드시 연결을 종료한다.
            if (conn != null) {
                conn.disconnect();
            }
        }

        return result;
    }

    /*
     * JSON 문자열에서 특정 key의 값을 간단히 꺼내는 메서드다.
     * 지금은 연습용 단순 파싱 방식이다.
     * 실무에서는 ObjectMapper 같은 JSON 파서를 사용하는 것이 좋다.
     */
    private String extractValue(String json, String key) {

        // 응답이 없으면 빈 문자열을 반환한다.
        if (json == null || "null".equals(json)) {
            return "";
        }

        // 찾을 패턴을 만든다. 예: "RESULT_CD":"
        String findText = "\"" + key + "\":\"";

        // 해당 key가 시작되는 위치를 찾는다.
        int startIndex = json.indexOf(findText);

        // key가 없으면 빈 문자열을 반환한다.
        if (startIndex < 0) {
            return "";
        }

        // 실제 값이 시작되는 위치로 이동한다.
        startIndex = startIndex + findText.length();

        // 값이 끝나는 큰따옴표 위치를 찾는다.
        int endIndex = json.indexOf("\"", startIndex);

        // 끝 위치를 못 찾으면 빈 문자열을 반환한다.
        if (endIndex < 0) {
            return "";
        }

        // 시작 위치부터 끝 위치 전까지 잘라서 값을 반환한다.
        return json.substring(startIndex, endIndex);
    }
}