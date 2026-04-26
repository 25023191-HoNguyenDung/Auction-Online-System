package contract_server;

import com.fasterxml.jackson.databind.JsonNode;

//record: class những ngắn hơn, tự động sinh code(getter,setter,...).
public record MessageEnvelope(
        String protocolVersion, // phiên bản giao thức : giúp client/server biết có tương thích hay không
        String messageId, // id riêng của mỗi message
        MessageType type, //loại message
        String timestamp, // thời điểm tạo/gửi message giúp sắp xếp thứ tự
        String correlationId, // kết nỗi yêu cầu gửi đi và kết quả trả về giúp không bị nhầm lẫn
        JsonNode payload // dữ liệu chính của message
) {}