package ru.nikita.openabeapi;

import ru.nikita.openabeapi.dao.MasterKeysObj;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;

public class OpenABEApi {

    Properties prop = new Properties();
    InputStream input;

    private final String OPENABE_PATH;


    /**
     * Чтение параметров запуска при инициализации
     * @throws IOException
     */
    public OpenABEApi() throws IOException {
        input = getClass().getClassLoader().getResourceAsStream("config.properties");
        prop.load(input);
        OPENABE_PATH = prop.getProperty("openabe.path");
    }

    /**
     * Устанавливает активный мастер-ключ
     * @param masterKeysObj
     * @param schema
     * @param name
     * @return
     */
    public boolean setMasterKeys(MasterKeysObj masterKeysObj, Schema schema, String name){
        try {
            String filePostf = schema.name().toLowerCase() + "abe";
            Files.write(Paths.get(OPENABE_PATH + "/" + name + ".mpk." + filePostf), masterKeysObj.getPublicKey().getBytes());
            Files.write(Paths.get(OPENABE_PATH + "/" + name + ".msk." + filePostf), masterKeysObj.getSecretKey().getBytes());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Удаляет активный мастер-ключ
     * @param schema
     * @param name
     * @return
     */
    public String remuveMasterKey(Schema schema, String name){
        String filePostf = schema.name().toLowerCase() + "abe";
        try {
            Files.deleteIfExists(Paths.get(OPENABE_PATH + "/" + name + ".mpk." + filePostf));
            if(Files.deleteIfExists(Paths.get(OPENABE_PATH + "/" + name + ".msk." + filePostf))) return "remove";
            else return "not exist";
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
    }

    /**
     * Генерация мастер ключа
     * @param schema
     * @param name
     * @return
     */
    public MasterKeysObj generateMasterKeys(Schema schema, String name){
        Runtime run = Runtime.getRuntime();
        try {
            String comand = OPENABE_PATH + "/oabe_setup -s " + schema.name() + " -p "+ OPENABE_PATH + "/" + name;
            run.exec(new String[]{"bash", "-c", comand}).waitFor();
            String filePostf = schema.name().toLowerCase() + "abe";
            MasterKeysObj masterKeysObj = new MasterKeysObj();
            masterKeysObj.setPublicKey(new String(Files.readAllBytes(Paths.get(OPENABE_PATH + "/" + name + ".mpk." + filePostf))));
            masterKeysObj.setSecretKey(new String(Files.readAllBytes(Paths.get(OPENABE_PATH + "/" + name + ".msk." + filePostf))));
            return masterKeysObj;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Генерация пользовательского ключа
     * @param schema
     * @param attributes
     * @param name
     * @return
     */
    public String generateUserKey(Schema schema, String attributes, String name){
        String fileName = UUID.randomUUID().toString();
        Runtime run = Runtime.getRuntime();
        try {
            String comand = OPENABE_PATH + "/oabe_keygen -s " + schema.name() +
                    " -p " + OPENABE_PATH + "/" + name +
                    " -i \"" + attributes + "\"" +
                    " -o " + OPENABE_PATH + "/" +fileName;
            run.exec(new String[]{"bash", "-c", comand}).waitFor();
            String result = new String(Files.readAllBytes(Paths.get(OPENABE_PATH + "/" + fileName + ".key")));
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            try {
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".key"));
            } catch (IOException e) {}
        }
        return null;
    }

    /**
     * Шифрование данных
     * @param schema
     * @param attributes
     * @param name
     * @param textForEncoding
     * @return
     */
    public String encoding(Schema schema, String attributes, String name, String textForEncoding){
        String fileName = UUID.randomUUID().toString();
        Runtime run = Runtime.getRuntime();
        try {
            Files.write(Paths.get( OPENABE_PATH + "/" + fileName + ".in"), textForEncoding.getBytes());
            String comand = OPENABE_PATH + "/oabe_enc -s " + schema.name() +
                    " -p " + OPENABE_PATH + "/"+  name +
                    " -e \"" + attributes + "\"" +
                    " -i " + OPENABE_PATH + "/" + fileName + ".in" +
                    " -o " + OPENABE_PATH + "/" + fileName + ".out";
            run.exec(new String[]{"bash", "-c", comand}).waitFor();
            return new String(Files.readAllBytes(Paths.get(OPENABE_PATH+ "/" + fileName + ".out." + schema.name().toLowerCase() + "abe")));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".in"));
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".out." + schema.name().toLowerCase() + "abe"));
            } catch (IOException e) {}
        }
        return null;
    }

    /**
     * Дешифрование файлов по имени ключа
     * @param schema
     * @param name
     * @param key
     * @param textForDecoding
     * @return
     */
    public String decoding(Schema schema, String name, String key, String textForDecoding){
        String fileName = UUID.randomUUID().toString();
        Runtime run = Runtime.getRuntime();
        try {
            Files.write(Paths.get(OPENABE_PATH + "/" +fileName + ".key"), key.getBytes());
            Files.write(Paths.get(OPENABE_PATH + "/" +fileName + ".in"), textForDecoding.getBytes());
            String comand = OPENABE_PATH + "/oabe_dec -s " + schema.name() +
                    " -p " + OPENABE_PATH + "/" + name +
                    " -k " + OPENABE_PATH + "/" + fileName + ".key" +
                    " -i " + OPENABE_PATH + "/" + fileName + ".in" +
                    " -o " + OPENABE_PATH + "/" + fileName + ".out";

            run.exec(new String[]{"bash", "-c", comand}).waitFor();
            if(Files.exists(Paths.get(OPENABE_PATH + "/" + fileName + ".out"))) {
                String s = new String(Files.readAllBytes(Paths.get(OPENABE_PATH + "/" + fileName + ".out")));
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".out"));
                return s;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".key"));
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".in"));
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".out"));
            } catch (IOException e) {}
        }
        return null;
    }

    /**
     * Дешифрование файлов по объекту файла ключа в системе
     * @param schema
     * @param name
     * @param keyFile
     * @param textForDecoding
     * @return
     */
    public String  decoding(Schema schema, String name, File keyFile, String textForDecoding){
        String fileName = UUID.randomUUID().toString();
        Runtime run = Runtime.getRuntime();
        try {
            Files.write(Paths.get(OPENABE_PATH + "/" +fileName + ".in"), textForDecoding.getBytes());
            String comand = OPENABE_PATH + "/oabe_dec -s " + schema.name() +
                    " -p " + OPENABE_PATH + "/" + name +
                    " -k " + keyFile.getAbsolutePath() +
                    " -i " + OPENABE_PATH + "/" + fileName + ".in" +
                    " -o " + OPENABE_PATH + "/" + fileName + ".out";
            run.exec(new String[]{"bash", "-c", comand}).waitFor();
            if(Files.exists(Paths.get(OPENABE_PATH + "/" + fileName + ".out"))) {
                String s = new String(Files.readAllBytes(Paths.get(OPENABE_PATH + "/" + fileName + ".out")));
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".out"));
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".in"));
                return s;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".key"));
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".in"));
                Files.delete(Paths.get(OPENABE_PATH + "/" + fileName + ".out"));
            } catch (IOException e) {}
        }
        return null;
    }

}
