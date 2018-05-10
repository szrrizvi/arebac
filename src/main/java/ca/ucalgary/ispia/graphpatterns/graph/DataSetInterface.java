package ca.ucalgary.ispia.graphpatterns.graph;

public class DataSetInterface {
	private MyNode[][] matrixOut;
	private MyNode[][] matrixIn;
	
	public DataSetInterface(DataSet dataSet){
		
		int numNodes = dataSet.getNodes().size();
		int numRelTypes = dataSet.getRelTyeps().size();
		matrixOut = new MyNode[numNodes][numRelTypes];
		matrixIn = new MyNode[numNodes][numRelTypes];
	}
	
	private void populateMatrix(DataSet dataSet){
		
	}
	
	
}
