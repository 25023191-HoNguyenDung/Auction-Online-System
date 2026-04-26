package contract_server;
// dữ liệu trả về khi có lỗi
import java.util.Map;

public record ErrorPayload(ErrorCode code, String message, Map<String, Object> details) {
}