package fitnesse.wiki.search;

import fitnesse.components.TraversalListener;
import fitnesse.wiki.NoPruningStrategy;
import fitnesse.wiki.SymbolicPage;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageProperty;
import fitnesse.wiki.WikiSourcePage;
import fitnesse.wiki.WikiWordReference;
import fitnesse.wikitext.SyntaxTree;
import fitnesse.wikitext.parser.ParsingPage;
import fitnesse.wikitext.parser.SymbolProvider;
import fitnesse.wikitext.parser.SyntaxTreeV2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WhereUsedPageFinder implements TraversalListener<WikiPage>, PageFinder {

  private final WikiPage subjectPage;
  private final TraversalListener<? super WikiPage> observer;
  private WikiPage currentPage;

  private final List<WikiPage> hits = new ArrayList<>();

  public WhereUsedPageFinder(WikiPage subjectPage, TraversalListener<? super WikiPage> observer) {
    this.subjectPage = subjectPage;
    this.observer = observer;
  }

  @Override
  public void process(WikiPage currentPage) {
    this.currentPage = currentPage;

    checkSymbolicLinks();
    if (!hits.contains(currentPage)) {
      checkContent();
    }
  }

  private void checkSymbolicLinks() {
    WikiPageProperty suiteProperty = currentPage.getData().getProperties().getProperty(SymbolicPage.PROPERTY_NAME);
    if (suiteProperty != null) {
      Set<String> links = suiteProperty.keySet();
      for (String link : links) {
        WikiPage linkTarget = currentPage.getChildPage(link);
        if (linkTarget instanceof SymbolicPage
          && ((SymbolicPage) linkTarget).getRealPage().equals(subjectPage)) {
          addHit();
          break;
        }
      }
    }
  }

  private void checkContent() {
    String content = currentPage.getData().getContent();
    SyntaxTree syntaxTree = new SyntaxTreeV2(SymbolProvider.refactoringProvider);
    syntaxTree.parse(content, new ParsingPage(new WikiSourcePage(currentPage)));
    syntaxTree.findWhereUsed(name -> {
      WikiPage referencedPage = new WikiWordReference(currentPage, name).getReferencedPage();
      if (referencedPage != null && referencedPage.equals(subjectPage)) {
        addHit();
      }
    });
  }

  @Override
  public void search(WikiPage page) {
    hits.clear();
    page.getPageCrawler().traverse(this, new NoPruningStrategy());
  }

  private void addHit() {
    if (hits.contains(currentPage)) return;
    hits.add(currentPage);
    observer.process(currentPage);
  }
}
