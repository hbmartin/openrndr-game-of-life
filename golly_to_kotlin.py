import sys

def process_files(file_name):
    result = []
    with open(file_name, 'r') as file:
        for line in file:
            line = line.strip()
            if line and not line.startswith('#') and not line.startswith('x'):
                result.append(line.replace("$", "\\$"))
    return ''.join(result)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python script.py <file1> <file2> ...")
        sys.exit(1)

    file_names = sys.argv[1:]
    outputs = {}
    for file_name in file_names:
        outputs["".join([t.title() for t in file_name.split(".")[0].split("/")[1].split("-")])] = process_files(file_name)

    print("enum class Patterns(val value: String) {")
    for name, pattern in outputs.items():
        if len(pattern) < 5000:
            print(f'    {name}("{pattern}"),')
    print("}")
