package com.stroexd.hsdecktracker.core.fusion;

// Namespace-only copy of HS Post-match Exporter v0.4 PowerParser.java.
// Provenance/hash and unchanged parser limits: checkpoints/mobile-fusion-r5-post-match.md.

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Evidence export only: retains source lines, never infers hidden cards or choices. */
public final class PowerParser {
 private static final Pattern LINE=Pattern.compile("^\\w\\s+(\\S+)\\s+(GameState|PowerTaskList)\\.DebugPrintPower\\(\\) -\\s*(.*)$");
 private static final Pattern CHOICE_LINE=Pattern.compile("^\\w\\s+(\\S+)\\s+GameState\\.(DebugPrintEntityChoices|DebugPrintEntitiesChosen|SendChoices)\\(\\) -\\s*(.*)$");
 private static final Pattern CHOICE_ID=Pattern.compile("^id=(\\d+)(?: |$).*");
 private static final Pattern TAG=Pattern.compile("TAG_CHANGE Entity=(.*?) tag=(\\S+) value=(.*)");
 private static final Pattern ENTITY=Pattern.compile("(?:FULL_ENTITY - Creating ID=(\\d+)|(?:SHOW_ENTITY|CHANGE_ENTITY) - Updating Entity=.*? id=(\\d+).*?) CardID=(\\S*)");
 private static final Pattern BLOCK=Pattern.compile("BLOCK_START BlockType=(\\S+) Entity=(.*?) EffectCardId=.*? Target=(.*?) SubOption=.*");
 public static final class Result {
  public final List<Map<String,Object>> matches=new ArrayList<>();
  public final List<String> warnings=new ArrayList<>();
  public String stream;
  public boolean sourceTruncated;
 }
 public static Result parse(File source) throws IOException {
  Result result=new Result(); boolean gameState=false;
  try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(source),StandardCharsets.UTF_8))){String line;while((line=in.readLine())!=null){Matcher m=LINE.matcher(line);if(m.matches() && m.group(2).equals("GameState")){gameState=true;break;}}}
  result.stream=gameState?"GameState":"PowerTaskList";
  Map<String,Object> current=null;List<Map<String,Object>> events=null;Map<String,Map<String,Object>> choices=new LinkedHashMap<>();Map<String,Object> activeChoice=null;String activeMethod=null;int number=0;int ignored=0;
  try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(source),StandardCharsets.UTF_8))){String line;while((line=in.readLine())!=null){number++;
   if(line.contains("Truncating log, which has reached the size limit")){result.sourceTruncated=true;if(current!=null){current.put("source_truncated",true);current.put("truncation_source_line",number);}result.warnings.add("Hearthstone stopped source logging at line "+number+": "+line.trim()+". Data after this marker is unavailable.");}
   Matcher choiceLine=CHOICE_LINE.matcher(line);
   if(choiceLine.matches() && current!=null){String method=choiceLine.group(2),body=choiceLine.group(3);Matcher header=CHOICE_ID.matcher(body);
    if(header.matches()){String id=header.group(1);activeChoice=choices.get(id);if(activeChoice==null){activeChoice=new LinkedHashMap<>();activeChoice.put("choice_id",id);activeChoice.put("options",new ArrayList<String>());activeChoice.put("submitted_entities",new ArrayList<String>());activeChoice.put("confirmed_entities",new ArrayList<String>());activeChoice.put("evidence",new ArrayList<Map<String,Object>>());choices.put(id,activeChoice);choiceList(current).add(activeChoice);}activeMethod=method;
     if(method.equals("DebugPrintEntityChoices")){activeChoice.put("log_time",choiceLine.group(1));activeChoice.put("source_line",number);field(body,"ChoiceType",activeChoice,"choice_type");field(body,"Player",activeChoice,"player");field(body,"CountMin",activeChoice,"count_min");field(body,"CountMax",activeChoice,"count_max");}
     else activeChoice.put(method.equals("SendChoices")?"submitted_log_time":"confirmed_log_time",choiceLine.group(1));
    }
    if(activeChoice!=null && method.equals(activeMethod)){Map<String,Object> evidence=new LinkedHashMap<>();evidence.put("source_line",number);evidence.put("log_time",choiceLine.group(1));evidence.put("method",method);evidence.put("raw",body);choiceEvidence(activeChoice).add(evidence);
     if(body.startsWith("Source="))activeChoice.put("source",body.substring(7));
     else if(body.startsWith("Entities[") || body.startsWith("m_chosenEntities[")){int equals=body.indexOf('=');if(equals>=0){String key=method.equals("DebugPrintEntityChoices")?"options":method.equals("SendChoices")?"submitted_entities":"confirmed_entities";choiceEntities(activeChoice,key).add(body.substring(equals+1));}}
    }
    continue;
   }
   // Choice continuation lines must follow their own method; power records do not reset choice IDs.
   Matcher m=LINE.matcher(line);if(!m.matches() || !m.group(2).equals(result.stream))continue;
   String time=m.group(1),body=m.group(3);
   if(body.equals("CREATE_GAME")){current=new LinkedHashMap<>();events=new ArrayList<>();current.put("index",result.matches.size()+1);current.put("started_log_time",time);current.put("ended_log_time",null);current.put("completed",false);current.put("events",events);current.put("choices",new ArrayList<Map<String,Object>>());current.put("source_truncated",false);choices=new LinkedHashMap<>();activeChoice=null;activeMethod=null;result.matches.add(current);}
   if(current==null){ignored++;continue;}
   Map<String,Object> event=new LinkedHashMap<>();event.put("sequence",events.size()+1);event.put("source_line",number);event.put("log_time",time);event.put("raw",body);String kind=body.split(" ",2)[0];event.put("kind",kind);
   Matcher tag=TAG.matcher(body);if(tag.matches()){event.put("entity",tag.group(1));event.put("tag",tag.group(2));event.put("value",tag.group(3).trim());if(tag.group(2).equals("STEP") && tag.group(3).trim().equals("FINAL_GAMEOVER")){current.put("completed",true);current.put("ended_log_time",time);}}
   Matcher identity=ENTITY.matcher(body);if(identity.matches()){event.put("entity_id",identity.group(1)!=null?identity.group(1):identity.group(2));event.put("card_id",identity.group(3));}
   Matcher block=BLOCK.matcher(body);if(block.matches()){event.put("block_type",block.group(1));event.put("actor",block.group(2));event.put("target",block.group(3));}
   if(events.size()>=100000)throw new IOException("Match exceeds 100,000 event limit; retain raw source only");
   events.add(event);
  }}
  if(result.matches.isEmpty())result.warnings.add("No CREATE_GAME in selected stream; no matches decoded. Raw source retained.");
  if(ignored>0)result.warnings.add(ignored+" selected-stream lines precede first CREATE_GAME; leading partial match not decoded.");
  for(Map<String,Object> match:result.matches)if(!Boolean.TRUE.equals(match.get("completed")))result.warnings.add("Match "+match.get("index")+" has no FINAL_GAMEOVER marker; unfinished or truncated.");
  result.warnings.add("Completion markers do not prove gap-free capture. Hidden identities remain unknown; no full board replay or deck inference.");
  result.warnings.add("Choice records retain observed options, submitted selections and server-confirmed selections separately. GENERAL does not by itself prove a Discover effect; mulligan keep/replace semantics are not inferred. Hidden identities remain unknown.");
  result.warnings.add("Log times have no timezone; midnight rollover is not resolved. Account identifiers may be present.");
  return result;
 }
 private static void field(String text,String name,Map<String,Object> output,String key){Matcher m=Pattern.compile("(?:^| )"+name+"=(.*?)(?= [A-Za-z]+[A-Za-z0-9]*=|$)").matcher(text);if(m.find())output.put(key,m.group(1));}
 @SuppressWarnings("unchecked") private static List<Map<String,Object>> choiceList(Map<String,Object> match){return (List<Map<String,Object>>)match.get("choices");}
 @SuppressWarnings("unchecked") private static List<Map<String,Object>> choiceEvidence(Map<String,Object> choice){return (List<Map<String,Object>>)choice.get("evidence");}
 @SuppressWarnings("unchecked") private static List<String> choiceEntities(Map<String,Object> choice,String key){return (List<String>)choice.get(key);}
 @SuppressWarnings("unchecked") public static String report(Map<String,Object> match,Result result){StringBuilder s=new StringBuilder("# Match "+match.get("index")+"\n\nStart: "+match.get("started_log_time")+"\nEnd: "+match.get("ended_log_time")+"\nEnd marker observed: "+match.get("completed")+"\n\n");
  for(Map<String,Object> e:(List<Map<String,Object>>)match.get("events")){
   if("PLAY".equals(e.get("block_type")) || "ATTACK".equals(e.get("block_type")))s.append("- ").append(e.get("log_time")).append(" | line ").append(e.get("source_line")).append(" | ").append(e.get("block_type")).append(": ").append(e.get("actor")).append(" → ").append(e.get("target")).append('\n');
   else if("TURN".equals(e.get("tag")) || "PLAYSTATE".equals(e.get("tag")))s.append("- ").append(e.get("log_time")).append(" | line ").append(e.get("source_line")).append(" | ").append(e.get("entity")).append(": ").append(e.get("tag")).append('=').append(e.get("value")).append('\n');
  }
  if(Boolean.TRUE.equals(match.get("source_truncated")))s.append("\nSOURCE TRUNCATED BY HEARTHSTONE. Match ending and later actions are missing.\n");
  s.append("\n## Observed choices\n");for(Map<String,Object> choice:choiceList(match)){s.append("\n- Choice ").append(choice.get("choice_id")).append(" | ").append(choice.get("choice_type")).append(" | ").append(choice.get("log_time")).append(" | source: ").append(choice.get("source")).append('\n');for(String option:choiceEntities(choice,"options"))s.append("  Option: ").append(option).append('\n');for(String submitted:choiceEntities(choice,"submitted_entities"))s.append("  Submitted: ").append(submitted).append('\n');for(String confirmed:choiceEntities(choice,"confirmed_entities"))s.append("  Confirmed: ").append(confirmed).append('\n');}
  s.append("\n## Limits\n");for(String warning:result.warnings)s.append("- ").append(warning).append('\n');return s.toString();
 }
 public static String json(Object value){if(value==null)return "null";if(value instanceof Boolean || value instanceof Number)return value.toString();if(value instanceof Map){StringJoiner out=new StringJoiner(",","{","}");for(Map.Entry<?,?> e:((Map<?,?>)value).entrySet())out.add(json(e.getKey().toString())+":"+json(e.getValue()));return out.toString();}if(value instanceof Iterable){StringJoiner out=new StringJoiner(",","[","]");for(Object v:(Iterable<?>)value)out.add(json(v));return out.toString();}StringBuilder s=new StringBuilder("\"");for(char c:value.toString().toCharArray()){switch(c){case '\"':s.append("\\\"");break;case '\\':s.append("\\\\");break;case '\n':s.append("\\n");break;case '\r':s.append("\\r");break;case '\t':s.append("\\t");break;default:if(c<32)s.append(String.format("\\u%04x",(int)c));else s.append(c);}}return s.append('"').toString();}
}
