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
			System.out.println("ex) java KakaoBankTest.class /kakaobank"); 
			return;
		}
		
		String rootPath = args[0];

		Path readPath = Paths.get(rootPath, READ_FILE_NAME);
		if(!Files.exists(readPath)) {
			System.out.println("지정된 경로에 [comments.cvs]파일이  존재하지 않습니다.");
			return;
		}
		
		Path writePath = Paths.get(rootPath, RESULT_FILE_PATH);
		if(Files.exists(writePath)) Files.delete(writePath); Files.createFile(writePath);
		
		// 1. 처리
		Map<String, Long> wordCount = schoolCount(readPath);
		
		// 2. 출력
		writeFile(writePath, wordCount);
		
	}

	private static void writeFile(Path writePath, Map<String, Long> wordCount) throws IOException {
		
		// 2.1 학교카운트 line별 write
		wordCount.forEach( (k, v) -> 
			{
				try {
					// 2.1.1  라인단위로 학교명 + \t + 카운트 + \n write
					Files.write(writePath, (k + "\t" + v + "\n").getBytes(), StandardOpenOption.APPEND);
					System.out.println(k + " " + v);
				} catch (IOException e) {
					System.err.println("Files.write Error [Key(" + k + ")]" + e.getMessage());
				}
			}
		);
	}

	private static ConcurrentMap<String, Long> schoolCount(Path p) throws IOException {
		
		return Arrays.asList(
				// 1.1 파일 데이터 읽기
				new String(Files.readAllBytes(p), StandardCharsets.UTF_8)
				// 1.2 단어별로 split
				.split("\\PL+"))
				// 1.3 병렬 스트림
				.parallelStream()
				// 1.4 필터
				.filter(s -> {
					// SchoolType 해당하는 단어로 포함되거나 시작되지 않는 항목 검색 (예: 중학교, 고등학교)
					for(SchoolType type : SchoolType.values()) {
						if(s.contains(type.getType()) && !s.startsWith(type.getType())){
							return true;
						}
					}
					return false;
				})
				// 1.5 그룹핑 (학교명/카운트)
				.collect(Collectors.groupingByConcurrent(s -> {
					for(SchoolType schoolType : SchoolType.values()) {
						if(s.contains(schoolType.getType())){
							// 1.5.1 SchoolType 이후 단어는 삭제 (예: 중학교를 -> 중학교)
							s = s.substring(0, s.indexOf(schoolType.getType()) + schoolType.getType().length());
							
							// 1.5.2 SchoolType 단어 앞 2자리만 가져옴 (예 : 서울공연예술고등학교 -> 예술고등학교)
							s = s.indexOf(schoolType.getType()) >= 2 ? 
									s.substring(s.indexOf(schoolType.getType()) - 2) : s;
						}
					}
					return s;
					
				// 1.6 학교 그룹별 카운트
				}, Collectors.counting()));
	}

	
	enum SchoolType{
		
		ELEMENTARY("초등학교"),
		MIDDLE("중학교"),
		HIGH("고등학교"),
		UNIVERSITY("대학교"),
		GIRLS_MIDDLE("여중"),
		GIRLS_HIGH("여고"),
		WOMENS_UNIVERSITY("여대")
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
