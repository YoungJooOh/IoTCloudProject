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
