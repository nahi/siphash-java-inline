require 'csv'

width = 120

result = CSV.parse(ARGF.read)

def average(line)
  # drop top and bottom data
  perfs = line.compact.map { |e| e.to_i }.sort[1, 8]
  perfs.inject(0) { |r, e| r + e } / perfs.size.to_f
end

max = 0
data = []
result.each do |line|
  idx, func = line.slice!(0, 2)
  v = average(line)
  data << [idx, func, v]
  max = [max, v].max
end

symbols = ['-', '+', '*', '=', '/', '\\']
symbol_map = {}

puts 'len: ave time per hash in 0.8M calcs'
puts '=' * width
data.each do |idx, func, v|
  label = "(%d ns)" % [v / 100000]
  size = (width.to_f / max * v).to_i
  symbol = (symbol_map[func] ||= symbols.shift)
  printf "%03d: %15s %s %s\n", idx, func, symbol * size, label
end
