package org.openmetadata.service.search.indexes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openmetadata.schema.entity.services.APIService;
import org.openmetadata.service.Entity;
import org.openmetadata.service.search.models.SearchSuggest;

public record APIServiceIndex(APIService apiService) implements SearchIndex {

  @Override
  public List<SearchSuggest> getSuggest() {
    List<SearchSuggest> suggest = new ArrayList<>();
    suggest.add(SearchSuggest.builder().input(apiService.getName()).weight(5).build());
    suggest.add(
        SearchSuggest.builder().input(apiService.getFullyQualifiedName()).weight(5).build());
    return suggest;
  }

  @Override
  public Object getEntity() {
    return apiService;
  }

  public Map<String, Object> buildSearchIndexDocInternal(Map<String, Object> doc) {
    Map<String, Object> commonAttributes =
        getCommonAttributesMap(apiService, Entity.SEARCH_SERVICE);
    doc.putAll(commonAttributes);
    return doc;
  }
}
