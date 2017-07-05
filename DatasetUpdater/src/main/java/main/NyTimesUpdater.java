package main;

import static main.LogFieldFormatter.format;
import static main.LogFieldFormatter.pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class NyTimesUpdater extends Thread {

	ArrayList<String> dbpediaCompanyList = new ArrayList<String>();
	ArrayList<String> nytimesCompanyList = new ArrayList<String>();

	// FIXME: nytimes URI doğrusuyla değiştirilecek
	VirtGraph store = new VirtGraph("http://nytimes.com", "jdbc:virtuoso://localhost:1111", "dba", "dba");

	private Logger logger = LoggerFactory.getLogger(NyTimesUpdater.class);

	private void init() throws IOException {

		//FIXME: organization data resources'ın altına eklenecek
		BufferedReader br = new BufferedReader(
				new FileReader("/home/oylum/Desktop/workspace/SemanticCartago2/organisation_data.txt"));

		String line;
		int blankPosition;
		while ((line = br.readLine()) != null) {
			blankPosition = line.indexOf(" ");
			this.dbpediaCompanyList.add(line.substring(0, blankPosition));
			line = line.substring(blankPosition + 1);
			blankPosition = line.indexOf(" ");
			this.nytimesCompanyList.add(line.substring(0, blankPosition));
			line = line.substring(blankPosition + 1);

		}

	}

	public void run() {

		try {
			this.init();

			int queryCounter = 0;

			logger.debug(format(pair("time", LocalDateTime.now()), pair("dataset", "nytimes")),
					"All datasets are being updated");

			while (queryCounter < 550)// this.nytimesCompanyList.size())
			{
				logger.debug(format(pair("time", LocalDateTime.now()), pair("dataset", "nytimes")), "Dataset updated");

				queryCounter++;
				// TODO: Count sorgusu burada sisteme atılacak

				// FIXME: article count URI ile değiştirilecek
				Node firstPredicate = Node.createURI("http://articlecount.com/predicate");
				int articleCount = 0;
				for (int i = 0; i < this.nytimesCompanyList.size(); i++) {

					Node subject = Node.createURI(this.nytimesCompanyList.get(i));

					String query = "SELECT ?sameCompany ?count WHERE {<"
							+ dbpediaCompanyList.get(queryCounter).toString()
							+ "> <http://www.w3.org/2002/07/owl#sameAs> ?sameCompany. ?sameCompany <http://data.nytimes.com/elements/associated_article_count> ?count.}";

					VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, store);
					ResultSet results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution result = results.nextSolution();
						articleCount = result.get("count").asLiteral().getInt();

						store.delete(new Triple(subject, firstPredicate, NodeFactoryExtra.intToNode(articleCount)));

					}
					articleCount++;
					store.add(new Triple(subject, firstPredicate, NodeFactoryExtra.intToNode(articleCount)));
					logger.debug(format(pair("time", LocalDateTime.now()), pair("company", subject.getURI()),
							pair("dataset", "nytimes")), "Company data has been updated");
				}

				Thread.sleep(120000);

				logger.debug(format(pair("time", LocalDateTime.now()), pair("dataset", "nytimes")), "Dataset updated");
			}

			logger.debug(format(pair("time", LocalDateTime.now()), pair("dataset", "nytimes")),
					"All datasets has been updated");

		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}

	}

}