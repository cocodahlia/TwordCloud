package info.moonfire.twordcloud4j;

//Twitter4j
import twitter4j.*;

//Java標準のやつ
import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Fluent API
import org.apache.http.client.fluent.Request;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.util.function.Consumer;
import java.util.logging.Level;

public class app {

    public static void main(String args[]) throws MalformedURLException, IOException {

        Twitter twitter = new TwitterFactory().getInstance();

        //検索結果を格納する変数を設定
        QueryResult queryResult = null;
        List<String> result = new ArrayList<>();
        queryResult = getTweets(queryResult, twitter);

        //検索した結果のヒット数を標準出力
        System.out.println("ヒット数:" + queryResult.getTweets().size());

        //ツイートをリストに放り込む
        saveTweets(queryResult, result);

        Map<String, Integer> analizeTweet = new HashMap<>();

        for (String tweet : result) {
            String responseString = getAnalizeData(tweet);
            ObjectMapper mapper = new ObjectMapper();
            analizeTweet(mapper, responseString, analizeTweet);
        }


        fileWrite(analizeTweet);

    }

    private static void fileWrite(Map<String, Integer> analizeTweet) throws IOException {
        
        try (PrintWriter filewriter = new PrintWriter(new BufferedWriter(new FileWriter(new File("resultList.csv"))))) {
            analizeTweet.entrySet().stream().forEach((Map.Entry<String, Integer> hoge) -> {
                filewriter.write("\"" + hoge.getValue() + "\"" + "," + "\"" + hoge.getKey() + "\""+"\r\n");
                
                
            });
        }
    }

    private static String getAnalizeData(String tweet) throws UnsupportedEncodingException, IOException {
        String url = "http://jlp.yahooapis.jp/KeyphraseService/V1/extract?appid=dj0zaiZpPVlpbk1EVkFFSVA3TiZzPWNvbnN1bWVyc2VjcmV0Jng9OWM-&output=json&sentence=";
        String queryTweet = URLEncoder.encode(tweet, "UTF-8");
        String responseString = Request
                .Get(url + queryTweet)
                .execute()
                .returnContent()
                .asString();
        return responseString;
    }

    private static void analizeTweet(ObjectMapper mapper, String responseString, Map<String, Integer> analizeTweet) throws NumberFormatException {
        try {
            JsonNode rootNode = mapper.readTree(responseString);
            Iterator<String> fieldNames = rootNode.fieldNames();
            while (fieldNames.hasNext()) {
                String word = fieldNames.next();
                int point = Integer.parseInt(rootNode.path(word).toString());

                if (analizeTweet.containsKey(word)) {
                    point = analizeTweet.get(word) + point;
                }
                analizeTweet.put(word, point);

            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void showTweets(List<String> result) {
        result.stream().forEach((tw) -> {
            System.out.println(tw);
        });
    }

    private static void saveTweets(QueryResult queryResult, List<String> result) {
        //Listに検索結果を格納する
        queryResult.getTweets().stream().map((tweet) -> tweet.getText()).map((tw) -> {
            String regex = "@[0-9a-zA-Z_]{1,15}";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(tw);
            return m;
        }).map((m) -> m.replaceAll("")).forEach((regexResult) -> {
            result.add(regexResult);
        });
    }

    private static QueryResult getTweets(QueryResult queryResult, Twitter twitter) {
        String searchWord = getQuery();
        Query query = new Query();
        try {
            query.setQuery(searchWord);
            //とりあえず100件とる
            query.setCount(100);
            query.setLang("ja");
            //検索を実行
            queryResult = twitter.search(query);
        } catch (TwitterException e) {
            System.out.println(e);
        }
        return queryResult;
    }
    

    private static String getQuery() {
        Scanner sc = new Scanner(System.in);
        String searchWord = sc.next();

        return searchWord + " exclude:retweets";
    }

}
