package ca.ucalgary.ispia.graphpatterns.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AttributeTypes {

	private static List<String> intTypes = new ArrayList<String>();
	private static List<String> vertexAttributes = new ArrayList<String>();
	private static List<String> edgeAttributes = new ArrayList<String>();
	

	public static List<String> getVertexAttributes(){

		if (vertexAttributes.isEmpty()){
			Scanner scan = null;

			try {
				scan = new Scanner(new FileInputStream("labels.data"));
			} catch (FileNotFoundException e){
				return null;
			}

			while(scan.hasNextLine()){
				vertexAttributes.add(scan.nextLine());
			}

			scan.close();
		}

		return vertexAttributes;
	}

	public static List<String> getEdgeAttributes(){
		if (edgeAttributes.isEmpty()){
			edgeAttributes.add("weight");
		}
		return edgeAttributes;
	}

	public static boolean isIntType(String attrName){

		if (intTypes.isEmpty()){
			intTypes.add("id");
			intTypes.add("age");
			intTypes.add("detailed industry recode");
			intTypes.add("detailed occupation recode");
			intTypes.add("wage per hour");
			intTypes.add("capital gains");
			intTypes.add("capital losses");
			intTypes.add("dividends from stocks");
			intTypes.add("num persons worked for employer");
			intTypes.add("weeks worked in year");
			intTypes.add("own business or self employed");
			intTypes.add("veterans benefits");
			intTypes.add("year");
			intTypes.add("weight");
		}

		if (intTypes.contains(attrName)){
			return true;
		} else {
			return false;
		}
	}
}
