import json

def _helper_get_stop_words():
    with open('recipes.json','r') as f:
        data = f.read()

    pdata = json.loads(data)
    stop_words = {}
    for t in pdata['recipes']:
        tags = t['name'].lower().split()
        for g in tags:
            if g in stop_words.keys():
                stop_words[g] += 1
            else:
                stop_words[g] = 1

    sorted_dict = sorted(stop_words.items(), key=lambda kv: kv[1])


    for k,v in sorted_dict:
        # if v > 5:
        print('{} {}'.format(k,v))

