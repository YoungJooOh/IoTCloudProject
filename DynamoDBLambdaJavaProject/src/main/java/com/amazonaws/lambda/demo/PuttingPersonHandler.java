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