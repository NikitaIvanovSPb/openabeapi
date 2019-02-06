import ru.nikita.openabeapi.OpenABEApi;
import ru.nikita.openabeapi.dao.MasterKeysObj;
import org.junit.jupiter.api.Test;
import ru.nikita.openabeapi.Schema;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


public class OpenABEApiTest {

    @Test
    public void runtimeTest() throws IOException, InterruptedException {
        BufferedReader buf;
        try {
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec("pwd");
            pr.waitFor();
            buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = "";
            while ((line = buf.readLine()) != null) {
                System.out.println(line);
            }
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void setMasterKeys() {
        String name = "ru.nikita.openabeapi.test";
        try {
            OpenABEApi openABEApi = new OpenABEApi();
            MasterKeysObj masterKeysObj = new MasterKeysObj();
            masterKeysObj.setSecretKey("secret");
            masterKeysObj.setPublicKey("public");
            assertTrue(openABEApi.setMasterKeys(masterKeysObj, Schema.CP, "ru.nikita.openabeapi.test"));
            assertEquals("secret", new String(Files.readAllBytes(Paths.get("./" + name + ".msk.cpabe"))));
            assertEquals("public", new String(Files.readAllBytes(Paths.get("./" + name + ".mpk.cpabe"))));
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            assertTrue(removeMasterKeys(name));
        }
    }


    @Test
    void generateMasterKeys() throws IOException {
        String name = "javaTest";
        try {
            OpenABEApi openABEApi = new OpenABEApi();
            MasterKeysObj masterKeysObj = openABEApi.generateMasterKeys(Schema.CP, name);
            assertTrue(Files.exists(Paths.get("./" + name + ".mpk.cpabe")));
            assertTrue(Files.exists(Paths.get("./" + name + ".msk.cpabe")));
            assertNotNull(masterKeysObj);
        }finally {
            assertTrue(removeMasterKeys(name));
        }
    }

    boolean removeMasterKeys(String name){
        try {
            Files.delete(Paths.get("./" + name + ".mpk.cpabe"));
            Files.delete(Paths.get("./" + name + ".msk.cpabe"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Test
    void generateUserKey(){
        String name = "javaTest";
        try {
            OpenABEApi openABEApi = new OpenABEApi();
            MasterKeysObj masterKeysObj = openABEApi.generateMasterKeys(Schema.CP, name);
            String res = openABEApi.generateUserKey(Schema.CP, "hello|Counter=3", name);
            assertNotNull(res);
            assertNotEquals("" , res);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            assertTrue(removeMasterKeys(name));
        }
    }

    @Test
    void encoding(){
        String name = "javaTest";
        try {
            OpenABEApi openABEApi = new OpenABEApi();
            MasterKeysObj masterKeysObj = openABEApi.generateMasterKeys(Schema.CP, name);
            String res = openABEApi.encoding(Schema.CP, "Hello AND Counter == 3", name, "textForEnc");
            assertNotNull(res);
            assertNotEquals("" , res);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            assertTrue(removeMasterKeys(name));
        }
    }

    @Test
    void decoding() throws IOException {
        String name = "javaTest";
        try {
            OpenABEApi openABEApi = new OpenABEApi();
            MasterKeysObj masterKeysObj = openABEApi.generateMasterKeys(Schema.CP, name);
            String validKey = openABEApi.generateUserKey(Schema.CP, "hello|Counter=3", name);
            String invalidKey = openABEApi.generateUserKey(Schema.CP, "hello|Counter=4", name);
            String encodingText = openABEApi.encoding(Schema.CP, "hello and Counter == 3", name, "testText");
            Files.delete(Paths.get("./" + name + ".msk.cpabe"));
            String validDecodingText = openABEApi.decoding(Schema.CP, name, validKey, encodingText);
            assertEquals("testText", validDecodingText);
            String invalidDecodingText = openABEApi.decoding(Schema.CP, name, invalidKey, encodingText);
            assertNotEquals("testText" , invalidDecodingText);
        } finally {
            Files.delete(Paths.get("./" + name + ".mpk.cpabe"));
        }
    }
}
