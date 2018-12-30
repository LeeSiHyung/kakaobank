

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
	static final String[] SCHOOL_FREFIX_SUBTRING_2BYTE_ARRAY = {"여자","체육"};
	
	enum SchoolType{
		E("초등학교"), M("중학교"), H("고등학교"), U("대학교"), GM("여중"), GH("여고"), WU("여대");
		final String type;
		SchoolType(String type){
			this.type = type;
		}
		public String getType() {
			return type;
		}
	}
	
	public static void main(String[] args){
		
		if(args.length <= 0) {
			System.out.println("java argument에[comments.cvs]파일이 저장된 디렉토리 경로를 지정해주세요."); 
			return;
		}
		
		String rootPath = args[0];
		
		try {
			
			// 1. 처리
			Path readPath = Paths.get(rootPath, READ_FILE_NAME);
			if(!Files.exists(readPath)) {
				System.out.println("지정된 경로에 [comments.cvs]파일이  존재하지 않습니다.");
				return;
			}
			
			String contents = new String(Files.readAllBytes(readPath), StandardCharsets.UTF_8);
			
			Map<String, Long> schoolCount = getSchoolCountMap(contents);
			
			// 2. 출력
			Path writePath = Paths.get(rootPath, RESULT_FILE_PATH);
			if(Files.exists(writePath)) Files.delete(writePath); Files.createFile(writePath);
			writeFile(writePath, schoolCount);
			
		} catch (IOException e) {
			System.err.println("Files Operation Error " + e.getMessage());
		}
		
	}

	private static ConcurrentMap<String, Long> getSchoolCountMap(String word){
		
		return Arrays.asList(
				// 1.1 단어별로 split
				word.split("\\PL+"))
				// 1.2 병렬 스트림
				.parallelStream()
				// 1.3 필터
				.filter(s -> {
					for(SchoolType type : SchoolType.values()) {
						// 1.3.1 학교타입을 포함하거나, 학교명이 없는 경우는 제외 (예: OO중학교(O), 중학교(X) )
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
							// 1.4.1 학교명 이후단어 삭제 (예: 중학교OOOO -> 중학교)
							s = s.substring(0, s.indexOf(schoolType.getType()) + schoolType.getType().length());
							
							// 1.4.2 학교명 이전단어 추출 (예 : OO고등학교, OO대학교)
							boolean flag = false;
							for(String arr : SCHOOL_FREFIX_SUBTRING_2BYTE_ARRAY) {
								if(s.indexOf(arr) >= 2) {
									s = s.substring(s.indexOf(arr) - 2);
									flag = true;
								}
							}
							if(!flag && s.indexOf(schoolType.getType()) >= 2){
								s = s.substring(s.indexOf(schoolType.getType()) - 2);
							}
							
						}
					}
					return s;
					
				// 1.5 학교 그룹별 카운트
				}, Collectors.counting()));
	}
	
	private static void writeFile(Path writePath, Map<String, Long> wordCount){
		// 2.1 학교카운트 line별 write
		wordCount.forEach( (k, v) -> 
			{
				try {
					System.out.println(k + " " + v);
					// 2.1.1  라인단위로 학교명 + \t + 카운트 + \n write
					Files.write(writePath, (k + "\t" + v + "\n").getBytes(), StandardOpenOption.APPEND);
				} catch (IOException e) {
					System.err.println("Files write Error : " + e.getMessage());
				}
			}
		);
	}

}
