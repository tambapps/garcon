package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpResponse;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public abstract class DynamicEndpointDefinition<T> extends EndpointDefinition<T> {

  private final List<String> pathVariableNames;
  private final Pattern pathVariablePattern;

  public DynamicEndpointDefinition(ContentType accept, ContentType contentType, List<String> pathVariableNames,
      Pattern pathVariablePattern) {
    super(accept, contentType);
    this.pathVariableNames = pathVariableNames;
    this.pathVariablePattern = pathVariablePattern;
  }

  @Override
  public HttpResponse call(HttpExchangeContext context) {
    Map<String, String> pathVariables = new HashMap<>();
    Matcher matcher = pathVariablePattern.matcher(context.getPath());
    // if we got here, this means the path matched the dynamic endpoint regex
    matcher.matches();
    for (int i = 0; i < pathVariableNames.size(); i++) {
      // i + 1 because group 0 is whole match
      pathVariables.put(pathVariableNames.get(i), matcher.group(i + 1));
    }
    context.setPathVariables(pathVariables);
    return super.call(context);
  }

  @Override
  public DynamicEndpointDefinition<T> toDynamic(List<String> pathVariableNames, Pattern pattern) {
    return this;
  }
}
