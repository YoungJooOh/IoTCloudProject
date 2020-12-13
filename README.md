# IoTCloudProject

# IoT 클라우드 기말고사 프로젝트

##목차

1. 기능 설명
2. 구조도
3. 람다 함수


## 식기세척기
### 식기세척기의 문을 열고 식기를 넣고 다시 닫으면 식기를 세척한다. 내부 고장시에 사용자 또는 관리자에게 알려준다.

## 1. 기능설명
- 어플리케이션에서 등록된 식기세척기를 조회할 수 있다.
- 식기세척기의 내부 상태를 모니터링하기 위한 기계 작동 상태 및 내부 환경 정보를 DB에 저장한다.
- 내부 기계가 고장이 나면 사용자에게 SNS서비스를 제공한다.

## 2. 구조도
![캡처](https://user-images.githubusercontent.com/31908591/102013176-df400880-3d91-11eb-9676-6d92ccf918d2.JPG)

## 3. 람다 함수
DynamoDBLambdaJavaProject - 새로운 식기세척기 등록시 DB에 저장한다
DishWasher이라는 DB 테이블에 id, region(지역), lastname(성) 항목을 입력해서 저장한다.
```javascript

package com.amazonaws.lambda.demo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class PuttingPersonHandler implements RequestHandler<Person, String> {
    private DynamoDB dynamoDb;
    private String TABLE_NAME = "DishWasher";
    private String REGION = "ap-northeast-2";

    @Override
    public String handleRequest(Person input, Context context) {
        this.initDynamoDbClient();

        putData(input);
        return "Saved Successfully!!";
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(REGION).build();
         this.dynamoDb = new DynamoDB(client);
    }

    private PutItemOutcome putData(Person person) 
              throws ConditionalCheckFailedException {
                return this.dynamoDb.getTable(TABLE_NAME)
                  .putItem(
                    new PutItemSpec().withItem(new Item()
                            .withPrimaryKey("id",person.id)
                            .withString("Region", person.Region)
                            .withString("lastName", person.lastName)));
            }
}

class Person {
    public String Region;
    public String lastName;
    public int id;
}
'''

ListingDeviceLambdaJavaProject - 현재 등록된 식기세척기들의 정보를 불러온다
aws에 저장되어 있는 전체 식기세척기(디바이스) 정보를 불러온다. 항목으로는 thingname과 thingarn값이 있다.
```javascript
package com.amazonaws.lambda.demo;

import java.util.List;
import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.AWSIotClientBuilder;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ListThingsResult;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class ListingDeviceHandler implements RequestHandler<Object, String> {

    @Override
    public String handleRequest(Object input, Context context) {

        // AWSIot 객체를 얻는다. 
        AWSIot iot = AWSIotClientBuilder.standard().build();

        // ListThingsRequest 객체 설정. 
        ListThingsRequest listThingsRequest = new ListThingsRequest();

        // listThings 메소드 호출하여 결과 얻음. 
        ListThingsResult result = iot.listThings(listThingsRequest);

        // result 객체로부터 API 응답모델 문자열 생성하여 반
        return getResponse(result);
    }

    /**
     * ListThingsResult 객체인 result로 부터 ThingName과 ThingArn을 얻어서 Json문자 형식의
     * 응답모델을 만들어 반환한다.
     * {
     *  "things": [ 
     *       { 
     *          "thingName": "string",
     *          "thingArn": "string"
     *       },
     *       ...
     *     ]
     * }
     */
    private String getResponse(ListThingsResult result) {
        List<ThingAttribute> things = result.getThings();

        String response = "{ \"things\": [";
        for (int i =0; i<things.size(); i++) {
            if (i!=0) 
                response +=",";
            response += String.format("{\"thingName\":\"%s\", \"thingArn\":\"%s\"}", 
                                                things.get(i).getThingName(),
                                                things.get(i).getThingArn());

        }
        response += "]}";
        return response;
    }

}
```

LogDeviceLambdaJavaProject - 디바이스 내부 상태 로그값을 불러온다
해당 디바이스의 로그 값들을 DB를 참조해서 선택된 시간 사이의 데이터 값을 불러온다.
```javascript
package com.amazonaws.lambda.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.TimeZone;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LogDeviceHandler implements RequestHandler<Event, String> {

    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "Logging";

    @Override
    public String handleRequest(Event input, Context context) {
        this.initDynamoDbClient();

        Table table = dynamoDb.getTable(DYNAMODB_TABLE_NAME);

        long from=0;
        long to=0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

            from = sdf.parse(input.from).getTime() / 1000;
            to = sdf.parse(input.to).getTime() / 1000;
        } catch (ParseException e1) {
            e1.printStackTrace();
        }

        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("deviceId = :v_id and #t between :from and :to")
                .withNameMap(new NameMap().with("#t", "time"))
                .withValueMap(new ValueMap().withString(":v_id",input.device).withNumber(":from", from).withNumber(":to", to)); 

        ItemCollection<QueryOutcome> items=null;
        try {           
            items = table.query(querySpec);
        }
        catch (Exception e) {
            System.err.println("Unable to scan the table:");
            System.err.println(e.getMessage());
        }

        return getResponse(items);
    }

    private String getResponse(ItemCollection<QueryOutcome> items) {

        Iterator<Item> iter = items.iterator();
        String response = "{ \"data\": [";
        for (int i =0; iter.hasNext(); i++) {
            if (i!=0) 
                response +=",";
            response += iter.next().toJSON();
        }
        response += "]}";
        return response;
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();

        this.dynamoDb = new DynamoDB(client);
    }
}

class Event {
    public String device;
    public String from;
    public String to;
}
```

MonitoringLambda - 내부 고장(워터펌프)가 고장이 나면 SNS기능을 이용하여 사용자의 이메일로 알려준다
```javascript
package com.amazonaws.lambda.demo;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Monitoring implements RequestHandler<Object, String> {

	@Override
	public String handleRequest(Object input, Context context) {
	    context.getLogger().log("Input: " + input);
	    String json = ""+input;
	    JsonParser parser = new JsonParser();
	    JsonElement element = parser.parse(json);
	    JsonElement state = element.getAsJsonObject().get("state");
	    JsonElement reported = state.getAsJsonObject().get("reported");
//	    String temperature = reported.getAsJsonObject().get("temperature").getAsString();
	    String brightness = reported.getAsJsonObject().get("CDS").getAsString();
	    String waterpump = reported.getAsJsonObject().get("waterpump").getAsString();
	    double bright = Double.valueOf(brightness);
//	    double temp = Double.valueOf(temperature);

	    final String AccessKey="AKIARXOVQ7V2WCNVU256";
	    final String SecretKey="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";
	    final String topicArn="arn:aws:sns:ap-northeast-2:119096278389:temerature_warning_topic";

	    BasicAWSCredentials awsCreds = new BasicAWSCredentials(AccessKey, SecretKey);  
	    AmazonSNS sns = AmazonSNSClientBuilder.standard()
	                .withRegion(Regions.AP_NORTHEAST_2)
	                .withCredentials( new AWSStaticCredentialsProvider(awsCreds) )
	                .build();

	    final String msg = "*Currently Error!*\n" + "Your device waterpump is under" + bright + ".";
	    final String subject = "Dish Washer is Unfunctioning.";
	    if (waterpump == "error") {
	        PublishRequest publishRequest = new PublishRequest(topicArn, msg, subject);
	        PublishResult publishResponse = sns.publish(publishRequest);
	    }
	    
//	    final String msg = "*Temperature Critical*\n" + "Your device temperature is " + temp + "C";
//	    final String subject = "Critical Warning";
//	    if (temp >= 26.0) {
//	        PublishRequest publishRequest = new PublishRequest(topicArn, msg, subject);
//	        PublishResult publishResponse = sns.publish(publishRequest);
//	    }

	    return subject+ "brightness = " + brightness + "!";
//	    return subject+ "temperature = " + temperature + "!";
	}

}

```

RecordingDeviceDataJavaProject2 - 워터펌프가 고장나거나 수리가 된 경우에 해당 로그 값만을 DB에 저장한다.
```javascript
package com.amazonaws.lambda.demo;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class RecordingDeviceInfoHandler2 implements RequestHandler<Document, String> {
    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "Logging";

    @Override
    public String handleRequest(Document input, Context context) {
        this.initDynamoDbClient();
        context.getLogger().log("Input: " + input);

        //return null;
        return persistData(input);
    }

    private String persistData(Document document) throws ConditionalCheckFailedException {

        // Epoch Conversion Code: https://www.epochconverter.com/
        SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String timeString = sdf.format(new java.util.Date (document.timestamp*1000));

        // temperature와 LED 값이 이전상태와 동일한 경우 테이블에 저장하지 않고 종료 
        if (document.current.state.reported.waterpump.equals(document.previous.state.reported.waterpump)) {
                return null;
        }

        return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)
                .putItem(new PutItemSpec().withItem(new Item().withPrimaryKey("deviceId", document.device)
                        .withLong("time", document.timestamp)
                        .withString("CDS", document.current.state.reported.CDS)
                        .withString("water", document.current.state.reported.water)
                        .withString("waterpump", document.current.state.reported.waterpump)
                        .withString("timestamp",timeString)))
                .toString();
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion("ap-northeast-2").build();

        this.dynamoDb = new DynamoDB(client);
    }

}

/**
 * AWS IoT은(는) 섀도우 업데이트가 성공적으로 완료될 때마다 /update/documents 주제에 다음 상태문서를 게시합니다
 * JSON 형식의 상태문서는 2개의 기본 노드를 포함합니다. previous 및 current. 
 * previous 노드에는 업데이트가 수행되기 전의 전체 섀도우 문서의 내용이 포함되고, 
 * current에는 업데이트가 성공적으로 적용된 후의 전체 섀도우 문서가 포함됩니다. 
 * 섀도우가 처음 업데이트(생성)되면 previous 노드에는 null이 포함됩니다.
 * 
 * timestamp는 상태문서가 생성된 시간 정보이고, 
 * device는 상태문서에 포함된 값은 아니고, Iot규칙을 통해서 Lambda함수로 전달된 값이다. 
 * 이 값을 해당 규칙과 관련된 사물이름을 나타낸다. 
 */
class Document {
    public Thing previous;       
    public Thing current;
    public long timestamp;
    public String device;       // AWS IoT에 등록된 사물 이름 
}

class Thing {
    public State state = new State();
    public long timestamp;
    public String clientToken;

    public class State {
        public Tag reported = new Tag();
        public Tag desired = new Tag();

        public class Tag {
            public String CDS;
            public String water;
            public String waterpump;
        }
    }
}
```

RecordingDeviceDataLambdaJavaProject - 전체 로그값을 DB에 저장한다
```javascript
package com.amazonaws.lambda.demo;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class RecordingDeviceInfoHandler implements RequestHandler<Thing, String> {
    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "DishWasherData";

    @Override
    public String handleRequest(Thing input, Context context) {
        this.initDynamoDbClient();

        persistData(input);
        return "Success in storing to DB!";
    }

    private PutItemOutcome persistData(Thing thing) throws ConditionalCheckFailedException {

        // Epoch Conversion Code: https://www.epochconverter.com/
        SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String timeString = sdf.format(new java.util.Date (thing.timestamp*1000));

        return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)
                .putItem(new PutItemSpec().withItem(new Item().withPrimaryKey("time", thing.timestamp)
                        .withString("CDS", thing.state.reported.CDS)
                        .withString("water", thing.state.reported.water)
                        .withString("waterpump", thing.state.reported.waterpump)
                        .withString("timestamp",timeString)));
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion("ap-northeast-2").build();

        this.dynamoDb = new DynamoDB(client);
    }

}

class Thing {
    public State state = new State();
    public long timestamp;

    public class State {
        public Tag reported = new Tag();
        public Tag desired = new Tag();

        public class Tag {
            public String CDS;
            public String water;
            public String waterpump;
        }
    }
} 
```

UpdateDeviceLambdaJavaProject - 앱에서 디바이스를 제어할 수 있다. 
```javascript
package com.amazonaws.lambda.demo;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.annotation.JsonCreator;

public class UpdateDeviceHandler implements RequestHandler<Event, String> {

    @Override
    public String handleRequest(Event event, Context context) {
        context.getLogger().log("Input: " + event);

        AWSIotData iotData = AWSIotDataClientBuilder.standard().build();

        String payload = getPayload(event.tags);

        UpdateThingShadowRequest updateThingShadowRequest  = 
                new UpdateThingShadowRequest()
                    .withThingName(event.device)
                    .withPayload(ByteBuffer.wrap(payload.getBytes()));

        UpdateThingShadowResult result = iotData.updateThingShadow(updateThingShadowRequest);
        byte[] bytes = new byte[result.getPayload().remaining()];
        result.getPayload().get(bytes);
        String resultString = new String(bytes);
        return resultString;
    }

    private String getPayload(ArrayList<Tag> tags) {
        String tagstr = "";
        for (int i=0; i < tags.size(); i++) {
            if (i !=  0) tagstr += ", ";
            tagstr += String.format("\"%s\" : \"%s\"", tags.get(i).tagName, tags.get(i).tagValue);
        }
        return String.format("{ \"state\": { \"desired\": { %s } } }", tagstr);
    }

}

class Event {
    public String device;
    public ArrayList<Tag> tags;

    public Event() {
         tags = new ArrayList<Tag>();
    }
}

class Tag {
    public String tagName;
    public String tagValue;

    @JsonCreator 
    public Tag() {
    }

    public Tag(String n, String v) {
        tagName = n;
        tagValue = v;
    }
}
```

