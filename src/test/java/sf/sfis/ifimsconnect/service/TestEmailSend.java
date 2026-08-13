package sf.sfis.ifimsconnect.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class TestEmailSend {
    public static void main(String[] args) {
        String host = "192.168.10.11";
        String port = "1025";
        String toEmail = "pocwm1@esbv10.co.th";
        String fromEmail = "ifimsconnect@sfis.co.th";
        String hopo = "DMK";

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "false");
        properties.put("mail.smtp.starttls.enable", "false");

        Session session = Session.getInstance(properties);

        try {
            MimeMessage message = new MimeMessage(session);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("NACK - Error during process "+hopo+" AODB inbound message");
			message.setFrom(new InternetAddress(fromEmail, "iFIMSConnect", "UTF-8"));
            
            // ลองสร้าง XML สมมติขึ้นมาส่ง
            String mockXmlEsb = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + //
                                "<MSG>\n" + //
                                "    <MSGORIGIN xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n" + //
                                "    <TIME>20260803100006</TIME>\n" + //
                                "    <ACKTYPE>NACK</ACKTYPE>\n" + //
                                "    <NACKDETAIL>\n" + //
                                "        <faultcode>error</faultcode>\n" + //
                                "        <faultstring>Unable to create snapshot:</faultstring>\n" + //
                                "        <faultactor>FIDS:IF_ROOTAPP</faultactor>\n" + //
                                "        <faultdetail>ORA-01830: date format picture ends before converting entire input string\n" + //
                                "</faultdetail>\n" + //
                                "    </NACKDETAIL>\n" + //
                                "    <MESSAGE xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n" + //
                                "</MSG>";
            message.setText(mockXmlEsb, "UTF-8", "xml");

            Transport.send(message);
            System.out.println("✅ ส่งอีเมลสำเร็จแล้ว! ตรวจสอบที่ Mailbox ปลายทางได้เลย");
        } catch (Exception e) {
            System.err.println("❌ ส่งไม่ผ่าน เกิดข้อผิดพลาด:");
            e.printStackTrace();
        }
    }
}