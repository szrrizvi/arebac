package ca.ucalgary.ispia.graphpatterns.graph;

import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class MutualFriends {
	private String q0 = "PROFILE \n" 
			+ "MATCH (a {id:219 }) , (g {id:4282 }) \n"
			+ "MATCH (f:PERSON)<-[:FRIEND]- (c:PERSON) \n" 
			+ "WHERE f<>c \n"
			+ "MATCH (f:PERSON)<-[:FRIEND]- (d:PERSON) \n" 
			+ "WHERE f<>d AND c<>d \n"
			+ "MATCH (f:PERSON)<-[:FRIEND]- (b:PERSON) \n" 
			+ "WHERE f<>b AND c<>b AND d<>b \n"
			+ "MATCH (e:PERSON)-[:FRIEND]-> (a:PERSON) \n" 
			+ "WHERE f<>e AND f<>a AND c<>e AND c<>a AND d<>e AND d<>a AND b<>e AND b<>a AND e<>a \n"
			+ "MATCH (a:PERSON)<-[:FRIEND]- (b:PERSON)  \n"
			+ "MATCH (a:PERSON)<-[:FRIEND]- (g:PERSON)  \n"
			+ "WHERE f<>g AND c<>g AND d<>g AND b<>g AND e<>g AND a<>g \n"
			+ "MATCH (a:PERSON)<-[:FRIEND]- (c:PERSON)  \n"
			+ "MATCH (f:PERSON)<-[:FRIEND]- (e:PERSON)  \n"
			+ "MATCH (g:PERSON)<-[:FRIEND]- (f:PERSON)  \n"
			+ "MATCH (d:PERSON)-[:FRIEND]-> (a:PERSON)  \n"
			+ "RETURN b.id, a.id, c.id, d.id, e.id, f.id, g.id;";

	private String q1 = "PROFILE " 
			+ "MATCH (a {id:219 }) , (g {id:4282 }) " 
			+ "MATCH (a:PERSON)<-[:FRIEND]-(g:PERSON) "
			+ "WHERE a<>g "
			+ "MATCH (g:PERSON)<-[:FRIEND]-(f:PERSON) "
			+ "WHERE a<>f AND g<>f "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(e:PERSON) "
			+ "WHERE a<>e AND g<>e AND f<>e "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(e:PERSON) "  
			+ "MATCH (a:PERSON)<-[:FRIEND]-(b:PERSON) "
			+ "WHERE a<>b AND g<>b AND f<>b AND e<>b "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(b:PERSON) "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(c:PERSON) "
			+ "WHERE a<>c AND g<>c AND f<>c AND e<>c AND b<>c "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(c:PERSON) "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(d:PERSON) "
			+ "WHERE a<>d AND g<>d AND f<>d AND e<>d AND b<>d AND c<>d "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(d:PERSON)"
			+ "RETURN a.id, b.id, c.id, d.id, e.id, f.id, g.id;";

	private String q2 = "PROFILE " 
			+ "MATCH (a {id:219 }) , (g {id:4282 }) " 
			+ "MATCH (a:PERSON)<-[:FRIEND]-(e:PERSON) "
			+ "WHERE a<>e "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(b:PERSON) "
			+ "WHERE a<>b AND e<>b "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(c:PERSON) "
			+ "WHERE a<>c AND e<>c AND b<>c "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(d:PERSON) "  
			+ "WHERE a<>d AND e<>d AND b<>d AND c<>d "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(e:PERSON) "
			+ "WHERE a<>f AND e<>f AND b<>f AND c<>f AND d<>f "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(b:PERSON) "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(c:PERSON) "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(d:PERSON) "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(g:PERSON) "
			+ "WHERE a<>g AND e<>g AND b<>g AND c<>g AND d<>g AND f<>g "
			+ "MATCH (g:PERSON)<-[:FRIEND]-(f:PERSON) "
			+ "RETURN a.id, b.id, c.id, d.id, e.id, f.id, g.id;";

	private String q3 = "PROFILE " 
			+ "MATCH (f:PERSON)<-[:FRIEND]-(e:PERSON) " 
			+ "WHERE f<>e "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(b:PERSON) "
			+ "WHERE f<>b AND e<>b "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(c:PERSON) "
			+ "WHERE f<>c AND e<>c AND b<>c "
			+ "MATCH (f:PERSON)<-[:FRIEND]-(d:PERSON) "  
			+ "WHERE f<>d AND e<>d AND b<>d AND c<>d "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(e:PERSON) "
			+ "WHERE f<>a AND e<>a AND b<>a AND c<>a AND d<>a "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(b:PERSON) "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(c:PERSON) "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(d:PERSON) "
			+ "MATCH (f:PERSON)-[:FRIEND]->(g:PERSON) "
			+ "WHERE f<>g AND e<>g AND b<>g AND c<>g AND d<>g AND a<>g "
			+ "MATCH (a:PERSON)<-[:FRIEND]-(g:PERSON) "
			+ "MATCH (a {id:219 }) , (g {id:4282 })  "
			+ "RETURN a.id, b.id, c.id, d.id, e.id, f.id, g.id;";

	private GraphDatabaseService graphDb;

	public MutualFriends(GraphDatabaseService graphDb){
		this.graphDb = graphDb;
	}

	public void test1(){
		String[] queries = {q0, q1, q2, q3};

		for (int i = 0 ; i < queries.length; i++){
			for (int j = 0; j < 100; j++)
				try (Transaction tx = graphDb.beginTx()){
					long start = 0l, end=0l;

					start = System.nanoTime();
					Result result = graphDb.execute(queries[i]);

					if (result.hasNext()){
						Map<String, Object> row = result.next();
						end = System.nanoTime();

						/*Set<String> keys = row.keySet();
						for (String key : keys){
							System.out.println("Key: " + key + ", Value: " + row.get(key));
						}*/

					} else {
						end = System.nanoTime();
						System.out.println("No results");
					}

					System.out.println((end-start));
					
					tx.success();
				}
			System.out.println();
		}
	}
}
