import json
import http.client, urllib.request, urllib.parse, urllib.error, base64
import gzip

class DTDYWegmans(object):

    def __init__(self):
        self._headers = {'Subscription-Key': '6aed878acd38406797d3958075511dc2'}
        self._params = urllib.parse.urlencode
        self.conn = http.client.HTTPSConnection('api.wegmans.io')


    def get_all_recipes(self):
        self.conn.request("GET", "/meals/recipes?api-version=2018-10-18&%s" % self._params({}), "{body}", self._headers)
        response = self.conn.getresponse()
        data = gzip.decompress(response.read())
        print(data.decode('utf-8'))
        # with open('recipes.json', 'w') as f:
        #     f.write(data.decode('utf-8'))
        self.conn.close()

    def get_recipe_by_ids(self, list_ids):
        """
            Returns list of ids to fetch the recipe ingredients
        """
        data = []
        for id in list_ids:
            self.conn.request('GET', "/meals/recipes/{id}?api-version=2018-10-18&%s" % self._params({'id':id}), "{body}", self._headers)
            response = self.conn.getresponse()
            data.append(response.read())
            self.conn.close()
        return data


    def _load_json(self, filename):
        """
            loads json file
        """
        with open(filename, 'r') as f:
            data = f.read()
        return json.loads(data)


if __name__ == '__main__':
    dw = DTDYWegmans()
    ids = ['19315','6768','21388']
    # dw.get_all_recipes()
    print(dw.get_recipe_by_ids(ids))