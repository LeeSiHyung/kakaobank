package com.kakaobank.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


public class KakaoBankTest {
	
	static final String READ_FILE_NAME = "comments.csv";
	static final String RESULT_FILE_PATH = "result.txt";
	
	public static void main(String[] args) throws IOException {
		
		if(args.length <= 0) {
			System.out.println("java argument에[comments.cvs]파일이 저장된 디렉토리 경로를 지정해주세요."); 
			return;
		}
		
		String rootPath = args[0];
		
		// 1. 처리
		Path readPath = Paths.get(rootPath, READ_FILE_NAME);
		if(!Files.exists(readPath)) {
			System.out.println("지정된 경로에 [comments.cvs]파일이  존재하지 않습니다.");
			return;
		}
		
		byte[] fileBytes = null;
		try {
			fileBytes = Files.readAllBytes(readPath);
		} catch (IOException e) {
			System.err.println("Files.readAllBytes Error " + e.getMessage());
		}
		
		Map<String, Long> schoolCount = getSchoolCountMap(new String(fileBytes, StandardCharsets.UTF_8));
		
		// 2. 출력
		Path writePath = Paths.get(rootPath, RESULT_FILE_PATH);
		if(Files.exists(writePath)) Files.delete(writePath); Files.createFile(writePath);
		writeFile(writePath, schoolCount);
		
	}

	private static ConcurrentMap<String, Long> getSchoolCountMap(String word){
		
		return Arrays.asList(
				// 1.1 단어별로 split
				word.split("\\PL+"))
				// 1.2 병렬 스트림
				.parallelStream()
				// 1.3 필터
				.filter(s -> {
					// 1.3.1 SchoolType 해당하는 단어로 포함되거나 시작되지 않는 항목 검색 (예: 중학교, 고등학교)
					for(SchoolType type : SchoolType.values()) {
						if(s.contains(type.getType()) && !s.startsWith(type.getType())){
							return true;
						}
					}
					return false;
				})
				// 1.4 그룹핑 (학교명/카운트)
				.collect(Collectors.groupingByConcurrent(s -> {
					for(SchoolType schoolType : SchoolType.values()) {
						if(s.contains(schoolType.getType())){
							// 1.4.1 SchoolType 이후 단어는 삭제 (예: 중학교를 -> 중학교)
							s = s.substring(0, s.indexOf(schoolType.getType()) + schoolType.getType().length());
							
							// 1.4.2 각각 여자상업고등학교의 경우 여자나 체육의 경우 해당 단어에 맞게 6자리 4자리 2자리로 축소한다.
							if(s.indexOf(schoolType.getType()) >= 6 && s.indexOf("여자상업고등학교") > 0) {
								s = s.substring(s.indexOf(schoolType.getType()) - 6);
							}
							else if(s.indexOf(schoolType.getType()) >= 4 && (s.indexOf("여자") > 0 || s.indexOf("체육") > 0)) {
								s = s.substring(s.indexOf(schoolType.getType()) - 4);
							}
							else if(s.indexOf(schoolType.getType()) >= 2){
								s = s.substring(s.indexOf(schoolType.getType()) - 2);
							}
						}
					}
					return s;
					
				// 1.5 학교 그룹별 카운트
				}, Collectors.counting()));
	}
	
	private static void writeFile(Path writePath, Map<String, Long> wordCount) throws IOException {
		
		// 2.1 학교카운트 line별 write
		wordCount.forEach( (k, v) -> 
			{
				try {
					System.out.println(k + " " + v);
					// 2.1.1  라인단위로 학교명 + \t + 카운트 + \n write
					Files.write(writePath, (k + "\t" + v + "\n").getBytes(), StandardOpenOption.APPEND);
				} catch (IOException e) {
					System.err.println("Files.write Error : " + e.getMessage());
				}
			}
		);
	}
	
	enum SchoolType{
		ELEMENTARY("초등학교"),
		MIDDLE("중학교"),
		HIGH("고등학교"),
		UNIVERSITY("대학교"),
		GIRLS_MIDDLE("여중"),
		GIRLS_HIGH("여고"),
		WOMENS_UNIVERSITY("여대"),
		;
		
		final String type;
		SchoolType(String type){
			this.type = type;
		}
		
		public String getType() {
			return type;
		}
		
	}

}
