
text=File.open('2323.txt').read
text.gsub!(/\r\n?/, "\n")
text.each_line do |line|
	`echo "#{line}" | nc -4u -w1 127.0.0.1 1234`
end
