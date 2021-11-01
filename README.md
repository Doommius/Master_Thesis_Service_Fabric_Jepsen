# thesis
Masters thesis by Mark Jervelund on Jepsen test



Overleaf Doc: https://www.overleaf.com/2773368222ddtjxhhzckwf



Note about clojure generator. 
Clojure

Build list of operations contains
Id - > int
Operation > str, enum or int. ? Not sure yet. String will probably be easiest but int/enum is quicker.. 
Values -> list [] (key, value, expected) maybe have it as a dict? 

Operation:
   Id: int
   Type: str
   Values: 
            key: string
            Value: int
            Expected: int
   Return: future return value that can be used and compares to history. 

Then simply build a list of these. And convert it to json and send it off to the backend? 















using System;
using Newtonsoft.Json;
using System.Collections.Generic;

public class Program
{
public static void Main()
{
Console.WriteLine("Hello World");
List<KeyValuePair<string, List<int>>> result = new List<KeyValuePair<string, List<int>>>();
dynamic operationlist;
String transactionquery = "{\"transaction\":[{\"operation\":\"w\",\"key\":\"9992886\",\"value\":[31]},{\"operation\":\"w\",\"key\":\"9992887\",\"value\":[32,32]}]}";
operationlist = Newtonsoft.Json.JsonConvert.DeserializeObject(transactionquery);
foreach (var item in operationlist.transaction)
{
if (item.operation.Value == "w")
{
Console.Write(item);
result.Add(new KeyValuePair<string, List<int>>(item.value.ToString(), new List<int>()
{-99}));
Console.Write(item.value.GetType());
result.Add(new KeyValuePair<string, List<int>>(item.key, item.value.ToObject(typeof(List<int>))));
}
}

		foreach (var resultentry in result)
		{
			Console.Write(resultentry.ToString());
		}
	}
}