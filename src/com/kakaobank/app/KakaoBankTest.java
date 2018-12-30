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
			System.out.println("java argument��[comments.cvs]������ ����� ���丮 ��θ� �������ּ���."); 
			System.out.println("ex) java KakaoBankTest.class /kakaobank"); 
			return;
		}
		
		String rootPath = args[0];

		Path readPath = Paths.get(rootPath, READ_FILE_NAME);
		if(!Files.exists(readPath)) {
			System.out.println("������ ��ο� [comments.cvs]������  �������� �ʽ��ϴ�.");
			return;
		}
		
		Path writePath = Paths.get(rootPath, RESULT_FILE_PATH);
		if(Files.exists(writePath)) Files.delete(writePath); Files.createFile(writePath);
		
		// 1. ó��
		Map<String, Long> wordCount = schoolCount(readPath);
		
		// 2. ���
		writeFile(writePath, wordCount);
		
	}

	private static void writeFile(Path writePath, Map<String, Long> wordCount) throws IOException {
		
		// 2.1 �б�ī��Ʈ line�� write
		wordCount.forEach( (k, v) -> 
			{
				try {
					// 2.1.1  ���δ����� �б��� + \t + ī��Ʈ + \n write
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
				// 1.1 ���� ������ �б�
				new String(Files.readAllBytes(p), StandardCharsets.UTF_8)
				// 1.2 �ܾ�� split
				.split("\\PL+"))
				// 1.3 ���� ��Ʈ��
				.parallelStream()
				// 1.4 ����
				.filter(s -> {
					// SchoolType �ش��ϴ� �ܾ�� ���Եǰų� ���۵��� �ʴ� �׸� �˻� (��: ���б�, ����б�)
					for(SchoolType type : SchoolType.values()) {
						if(s.contains(type.getType()) && !s.startsWith(type.getType())){
							return true;
						}
					}
					return false;
				})
				// 1.5 �׷��� (�б���/ī��Ʈ)
				.collect(Collectors.groupingByConcurrent(s -> {
					for(SchoolType schoolType : SchoolType.values()) {
						if(s.contains(schoolType.getType())){
							// 1.5.1 SchoolType ���� �ܾ�� ���� (��: ���б��� -> ���б�)
							s = s.substring(0, s.indexOf(schoolType.getType()) + schoolType.getType().length());
							
							// 1.5.2 SchoolType �ܾ� �� 2�ڸ��� ������ (�� : ���������������б� -> ��������б�)
							s = s.indexOf(schoolType.getType()) >= 2 ? 
									s.substring(s.indexOf(schoolType.getType()) - 2) : s;
						}
					}
					return s;
					
				// 1.6 �б� �׷캰 ī��Ʈ
				}, Collectors.counting()));
	}

	
	enum SchoolType{
		
		ELEMENTARY("�ʵ��б�"),
		MIDDLE("���б�"),
		HIGH("����б�"),
		UNIVERSITY("���б�"),
		GIRLS_MIDDLE("����"),
		GIRLS_HIGH("����"),
		WOMENS_UNIVERSITY("����")
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
