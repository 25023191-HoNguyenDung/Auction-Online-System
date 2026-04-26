git statuspackage com.auction.common.protocol;
// dữ liệu trả về khi có lỗi
import java.util.Map;

public record ErrorPayload(ErrorCode code, String message, Map<String, Object> details) {
}